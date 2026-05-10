# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

MediHelp is a college group project (3-4 members) demonstrating microservice architecture for a personal health assistant. It is **not** a production app — favor pragmatic fixes and avoid over-engineering. The deployed instance lives on a single Azure VM that pulls from this repo via `git pull`; the VM is often deallocated to save credits, so verification frequently happens locally.

## Common commands

### Build & run (full stack, local)
```bash
docker compose -f docker-compose.infra.yml up -d         # 5x Postgres + Mongo + Redis + RabbitMQ
mvn clean package -DskipTests -T 1C                      # Build all 8 Java modules in parallel
bash deploy/scripts/start-all.sh                         # Start Eureka, Gateway, all services + AI
bash deploy/scripts/status.sh                            # Health-check every service
bash deploy/scripts/stop-all.sh                          # Tear down
bash deploy/scripts/logs.sh auth                         # Tail a specific service's log
```

### Build a single Java service
```bash
mvn -pl medihelp-auth-service -am clean package -DskipTests
java -jar medihelp-auth-service/target/medihelp-auth-service-1.0.0-SNAPSHOT.jar
```
`-am` ("also-make") is required because every service depends on `medihelp-common`.

### Chatbot service (Python)
```bash
cd medihelp-chatbot-service
pip install --user --break-system-packages -r requirements.txt
set -a && . ./.env && set +a       # loads PINECONE_API_KEY, GROQ_API_KEY
python3 store_index.py             # one-off: chunk PDFs → Pinecone (skip if index already populated)
python3 app.py                     # serves on :8086, registers with Eureka
```
The chatbot's own `.env` lives in `medihelp-chatbot-service/.env`, **not** the repo root. It holds `PINECONE_API_KEY`, `GROQ_API_KEY`, `PINECONE_INDEX_NAME`, optional `FRIEND_ADVISOR_URL`, optional `TESSERACT_CMD`. The root `.env` is unused by services after the cutover.

### Frontend
```bash
cd medihelp-frontend && npm install && ng serve   # http://localhost:4200
```

### Tests
There is **no test suite** — `mvn test` finds nothing. Verification is done by running the stack and hitting endpoints (see `deploy/scripts/status.sh` and the smoke flow below).

## Architecture (the parts that aren't obvious from the file tree)

### Inter-service communication has two channels — and only two
- **Synchronous:** clients hit the API Gateway (`:8080`), which uses Eureka client-side load balancing (`lb://service-name`) to route by path prefix. There are **no Feign clients**; services do not call each other synchronously.
- **Asynchronous:** RabbitMQ topic exchange `medihelp.events` (defined in `medihelp-common/src/main/java/com/medihelp/common/event/RabbitMQConfig.java`). Producers `convertAndSend` an event class; consumers use `@RabbitListener(queues = ...)`. Six event types: `UserRegistered`, `MedicationReminder`, `AppointmentReminder`, `EmergencySos`, `VitalsAnomaly`. Adding a cross-service interaction means adding an event, not a REST call.

### JWT flow — controllers never parse tokens
`medihelp-gateway/.../JwtAuthenticationFilter.java` validates the JWT, checks Redis for blacklisting (logout uses `jti`), and **strips the `Authorization` header**, replacing it with three trusted headers injected from JWT claims:
- `X-User-Id` (UUID, the JWT `sub`)
- `X-User-Email`
- `X-User-Role`

Downstream controllers read these via `@RequestHeader("X-User-Id") String userId` — **always** assume those headers are present and trustworthy on protected routes, and **never** parse JWTs in services other than `auth-service`. Public paths are listed in `PUBLIC_PATHS` in the filter.

### Database isolation: one Postgres per service
Each Java service has its own Postgres on a dedicated port (`5433` auth → `5437` notification) and connects via service-specific env vars: `AUTH_DB_URL`, `USER_DB_URL`, `HEALTH_DB_URL`, `PRESCRIPTION_DB_URL`, `NOTIFICATION_DB_URL`. There is no generic `DB_URL`. The health service additionally talks to MongoDB for mood journals and health records.

### Shared module
`medihelp-common` is the only shared Java code: `JwtUtil`, the event DTOs, the RabbitMQ exchange/queue/routing-key constants, and `ApiResponse`/exception classes. Adding a new event type requires editing `RabbitMQConfig.java` here so producers and consumers agree on routing keys.

### Chatbot service is the AI surface
- Python/Flask + LangChain, runs on `:8086`. Registers with Eureka via `eureka_register.py` (called from `app.py` after the route definitions). Gateway route `lb://chatbot-service` strips the `/api/v1/chatbot` prefix so `/api/v1/chatbot/get` hits Flask's `/get`.
- Five-step RAG pipeline in `run_pipeline()` (`app.py`): symptom extraction → disease ID via Pinecone (`general` namespace) → pure-LLM fallback if RAG returns "general" → nutrition table via Pinecone (`nutrition` namespace) → 3-layer severity (rules + keywords + LLM, worst-of) → home care plan. Skips home care for CRITICAL.
- All LLM calls go through Groq (`llama-3.3-70b-versatile` for chat, `llama-4-scout` for vision, `whisper-large-v3` for voice). The Pinecone index `medical-chatbot` holds embeddings of three PDFs in `data/`; rebuild via `python3 store_index.py`.
- `get_current_user()` reads the `X-User-Id` header injected by the gateway after JWT validation (same pattern as Java services). Falls back to a Flask-session UUID only when the service is run standalone.
- The chatbot has its own SQLite DB (`chat_history.db`) with `sessions`, `messages`, `nutrition_targets`, `user_profile` tables. The `user_profile` table is **per-chatbot context** for cultural-advice; the canonical user profile lives in user-service and is read via `GET /api/v1/users/me` when needed.

## Endpoint conventions

- All gateway-routed paths are prefixed `/api/v1/<service>/...` and the gateway maps them by predicate (`Path=/api/v1/auth/**`, etc.).
- `/api/v1/users/me` is the user's own profile (uses `X-User-Id` header). `/api/v1/users/{userId}` is for cross-user lookups.
- Vitals: `POST /api/v1/health/vitals` expects `{type, value, unit, recordedAt}` — single numeric value, not `systolic`/`diastolic` pairs.
- All Java actuators expose `/actuator/health`. The chatbot service implements the same path (`app.py` end) so health checks are uniform.

## Conventions for changes

- **Don't add Feign clients** between services. If service A needs data from service B, the answer is either: route through the gateway (treat it as an external call), or add a RabbitMQ event so B publishes state changes A can react to.
- **Don't bypass the gateway in code.** All `lb://` URIs are registered in `medihelp-gateway/src/main/resources/application.yml`. Adding a new service means adding a route there + registering with Eureka.
- **Service-local `.env` files hold only that service's secrets.** The chatbot service reads `medihelp-chatbot-service/.env` (`PINECONE_API_KEY`, `GROQ_API_KEY`). Database creds live in each service's `application.yml` with localhost defaults — do not move DB creds into `.env` unless you also update every service config. If you edit `.env` on Windows, run `sed -i 's/\r$//' .env` before sourcing or HTTP headers built from those values will fail.
- **`Resend` free tier only mails the verified owner address** (`witbyte111@gmail.com`). For local OTP flows, the OTP is also logged in `auth.log` — `grep "OTP generated"` to retrieve it during testing.
- **Never delete `.original` jars** under `target/` — they are produced by `spring-boot-maven-plugin` repackaging and the build cleans them up itself.

## Smoke-test flow (registration → JWT → protected call)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"u@x.local","password":"TestPass123!","firstName":"U"}'
OTP=$(grep "OTP generated for u@x.local" /tmp/auth.log | tail -1 | grep -oE '[0-9]{6}')
curl -X POST http://localhost:8080/api/v1/auth/verify-otp -H 'Content-Type: application/json' \
  -d "{\"email\":\"u@x.local\",\"otp\":\"$OTP\"}"
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"u@x.local","password":"TestPass123!"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me
```
A successful run also produces `Default profile created for user <uuid>` in `user.log` — the `UserRegisteredListener` consumes the registration event over RabbitMQ.

## Memory

Per-conversation memory lives at `~/.claude/projects/-home-subhra28-medihelp/memory/` and tracks build progress, run instructions, architecture decisions, and user profile. Treat memory as a point-in-time snapshot — verify against current code before acting on memory claims about file contents or feature status.
