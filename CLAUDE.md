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

### AI service (Python)
```bash
cd medihelp-ai-service
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
If `python3-venv` isn't installed, fall back to `pip install --user --break-system-packages -r requirements.txt` and run via `~/.local/bin/uvicorn`.

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

### AI service is a special citizen
- It is Python/FastAPI, runs on `:8000`, and is **not registered with Eureka** despite the README's diagram. The lifespan hook in `app/main.py` has placeholder comments where the registration was meant to go. As a result, gateway routes to `lb://ai-service` return 503 — the AI service must be hit directly during local dev, or the gateway route should be changed to a fixed URI.
- `app/services/gemini_service.py` has **mock fallbacks** (`_mock_triage_response`, `_mock_diet_response`, `_mock_exercise_response`) that return on any exception or when the API key is missing. This is intentional graceful degradation but **hides real Gemini errors** — when debugging, check the AI service log for `Gemini API error:` lines before assuming the integration works.

## Endpoint conventions

- All gateway-routed paths are prefixed `/api/v1/<service>/...` and the gateway maps them by predicate (`Path=/api/v1/auth/**`, etc.).
- `/api/v1/users/me` is the user's own profile (uses `X-User-Id` header). `/api/v1/users/{userId}` is for cross-user lookups.
- Vitals: `POST /api/v1/health/vitals` expects `{type, value, unit, recordedAt}` — single numeric value, not `systolic`/`diastolic` pairs.
- All Java actuators expose `/actuator/health`. The AI service implements the same path (`app/main.py:36`) so health checks are uniform.

## Conventions for changes

- **Don't add Feign clients** between services. If service A needs data from service B, the answer is either: route through the gateway (treat it as an external call), or add a RabbitMQ event so B publishes state changes A can react to.
- **Don't bypass the gateway in code.** All `lb://` URIs are registered in `medihelp-gateway/src/main/resources/application.yml`. Adding a new service means adding a route there + registering with Eureka.
- **`.env` is sourced by `start-all.sh` and contains only secrets** (`GEMINI_API_KEY`, `RESEND_API_KEY`). Database creds live in each service's `application.yml` with localhost defaults — do not move DB creds into `.env` unless you also update every service config. If you edit `.env` on Windows, run `sed -i 's/\r$//' .env` before sourcing or HTTP headers built from those values will fail.
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
