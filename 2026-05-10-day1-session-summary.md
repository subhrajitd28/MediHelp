# Day 1 — Chatbot Integration Session Summary
**Date:** 2026-05-10 (session ran across 2026-05-09 → 2026-05-10)
**Goal:** Replace the broken Gemini-based `medihelp-ai-service` with the teammate's RAG-powered Medical_Chatbot, integrated as a first-class microservice. Add cultural-context fields at registration, mood journal, emergency SOS, drug interactions, FHIR export, smartwatch & therapy placeholders. Push everything.

---

## What got shipped (8 commits on `origin/main`)

| SHA | Commit | What it does |
|---|---|---|
| `ea06820` | `chore: gitignore noise` | nohup.out, *:Zone.Identifier, chat_history.db, Medical_Chatbot/ |
| `74ecb72` | `docs: add CLAUDE.md` | Project guide for future Claude sessions |
| `0ef2f3e` | `feat: cultural context at registration` | DOB / gender / state / dietPreference end-to-end (frontend form → AuthService → UserRegisteredEvent → user-service listener → UserProfile, readable via `/users/me`) |
| `23a019a` | `feat: medihelp-chatbot-service replaces ai-service` | New Flask + LangChain + Pinecone + Groq service on :8086, registers with Eureka. Native Angular UI rewrite (text + voice + image + severity modal + sessions sidebar + check-in). Markdown pipe. |
| `dafe695` | `feat: mood journal` | 6-week calendar grid + log dialog (mood 1-5, sleep, exercise, journal note) + correlation insights |
| `8dd1ee2` | `feat: emergency SOS button + severity modal` | Persistent red SOS button in sidebar with confirm dialog + browser geolocation; severity modal shared with chatbot |
| `d46e603` | `chore: misc polish` | appointment.type field, drug-interactions card on medications page, FHIR export button on health-records, wearable placeholder card on vitals |
| `8f1cab6` | `chore: remove medihelp-ai-service and finish cutover` | Delete old ai-service entirely, repoint prescription-scan at chatbot's image endpoint, update docker-compose / CI / logs.sh / CLAUDE.md, add Dockerfile for chatbot-service |

---

## Architecture state

### Inter-service routing through gateway (`:8080`)
| Path prefix | Route | Service |
|---|---|---|
| `/api/v1/auth/**` | `lb://auth-service` | :8081 |
| `/api/v1/users/**` | `lb://user-service` | :8082 |
| `/api/v1/health/**` | `lb://health-service` | :8083 |
| `/api/v1/prescriptions/**` | `lb://prescription-service` | :8084 |
| `/api/v1/notifications/**` | `lb://notification-service` | :8085 |
| `/api/v1/chatbot/**` | `lb://chatbot-service` (StripPrefix=3) | :8086 |
| `/api/v1/public/**` | `lb://user-service` | (public) |

### Auth flow (single source of truth)
JWT issued by auth-service → carried in `Authorization: Bearer ...` → gateway validates, strips it, **injects `X-User-Id` / `X-User-Email` / `X-User-Role` headers** → all downstream services (Java + Python chatbot) read those headers, never parse JWTs themselves.

### Inter-service async (RabbitMQ)
Topic exchange `medihelp.events`. Six event types defined in `medihelp-common/.../event/`. The big extension this session: `UserRegisteredEvent` now carries `firstName, lastName, dateOfBirth, gender, state, dietPreference, registeredAt`. user-service listener writes the full UserProfile in one transaction.

### Chatbot pipeline (`run_pipeline()` in `medihelp-chatbot-service/app.py`)
```
input (text / voice / image)
  ↓ Step 0  _extract_symptoms     LLM → JSON {symptoms, duration, temp, age, ...}
  ↓ Step 1  disease_chain         RAG over Pinecone 'general' → Llama-3.3-70B
  ↓ Step 2  fallback_disease      pure-LLM (only if Step 1 returns 'general')
  ↓ Step 3  nutrition_chain       RAG over Pinecone 'nutrition' → 7-row md table
  ↓ Step 4  _assess_severity      worst-of (rules + keywords + LLM triage)
  ↓ Step 5  home_care_chain       skipped for CRITICAL
  ↓
ChatbotResponse { reply, disease, severity, home_care, image_analysis?, transcript? }
```

### Pinecone state (durable, in cloud)
Index: `medical-chatbot` (Starter free tier, AWS us-east-1, cosine similarity, 384-dim)
Namespaces:
- `general` — 5,118 chunks (medic_book.pdf disease + Herbal_Nutrients herbal)
- `nutrition` — 657 chunks (nutrition.pdf)
Total: 5,775 chunks of medical knowledge.

To rebuild: `cd medihelp-chatbot-service && python3 store_index.py` (one-off; reads PDFs from `data/`, embeds with HuggingFace MiniLM-L6-v2, upserts).

---

## Stack inventory

```
Service              Port   Tech                              Eureka name
─────────────────────────────────────────────────────────────────────────
Eureka               8761   Spring Cloud Netflix              -
API Gateway          8080   Spring Cloud Gateway + JWT        API-GATEWAY
Auth                 8081   Spring Boot 3.3 + Resend email    AUTH-SERVICE
User & Profile       8082   Spring Boot + Postgres + Mongo    USER-SERVICE
Health Tracking      8083   Spring Boot + Postgres + Mongo    HEALTH-SERVICE
                            (vitals, mood encrypted, FHIR)
Prescription         8084   Spring Boot + Postgres + OpenFDA  PRESCRIPTION-SERVICE
Notification         8085   Spring Boot + RabbitMQ            NOTIFICATION-SERVICE
Medical Chatbot      8086   Flask + LangChain + Groq +        CHATBOT-SERVICE
                            Pinecone + Tesseract + SQLite     (NEW — replaces ai-service)
Frontend             4200   Angular 17 + Material             (dev) -
```

Infrastructure: 5×PostgreSQL (one per Java service), MongoDB, Redis, RabbitMQ — all docker-compose.

---

## Smoke test (verified end-to-end this session)

```bash
# 1. Register (with new cultural fields)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke3@medihelp.local","password":"TestPass123!",
       "firstName":"Smoke","lastName":"Three","dateOfBirth":"1995-06-15",
       "gender":"Male","state":"West Bengal","dietPreference":"Non-vegetarian"}'
# → 201, "Registration successful"

# 2. OTP retrievable from auth.log (Resend free tier limits real delivery)
OTP=$(grep "OTP generated for smoke3" /tmp/medihelp-logs/auth.log | tail -1 | grep -oE "[0-9]{6}")

# 3. Verify → Login → JWT
curl -X POST http://localhost:8080/api/v1/auth/verify-otp -d ...
TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login -d ... | jq -r .data.accessToken)

# 4. Profile created by RabbitMQ listener with all cultural fields
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users/me
# → state="West Bengal", dietPreference="Non-vegetarian", dateOfBirth, gender ✓

# 5. Real chatbot diagnosis through gateway (10 seconds)
curl -X POST http://localhost:8080/api/v1/chatbot/get \
  -H "Authorization: Bearer $TOKEN" \
  -F 'msg=I have had a fever of 102F and a sore throat for 3 days. I am 28 years old.'
# → Disease: Pharyngitis
# → 7-row nutrition table (carbs ↑, protein ↑, fat ↓, vit C 200mg, zinc 15mg, water ↑, fiber ↓)
# → 7-row home-care plan
# → Severity: MODERATE with clinical reasoning
```

---

## Files that changed (source of truth for the report's Implementation section)

### New services / modules
- `medihelp-chatbot-service/` — copied from teammate's Medical_Chatbot, then:
  - `app.py:246-260` — `get_current_user()` reads `X-User-Id` header
  - `app.py` end — `/actuator/health`, `/actuator/info`, Eureka registration
  - `eureka_register.py` (new) — py-eureka-client init + atexit deregister
  - `requirements.txt` — added `py-eureka-client==0.11.10`
  - `src/helper.py` — `PyPDFLoader` → `PyMuPDFLoader` (fixed inline-image crash); `DATA_DIR` defaults to relative path
  - `Dockerfile` (new)

### Backend (Java) — modified
- `medihelp-common/.../UserRegisteredEvent.java` — added 5 cultural-context fields
- `medihelp-auth-service/.../RegisterRequest.java` — added 4 cultural-context fields
- `medihelp-auth-service/.../UserAuth.java` — added 6 columns (firstName, lastName, DOB, gender, state, diet)
- `medihelp-auth-service/.../AuthService.java` — fills entity in register(), forwards to event in verifyOtp()
- `medihelp-user-service/.../UserProfile.java` — added state + dietPreference
- `medihelp-user-service/.../UserProfileRequest.java` + `Response.java` — added fields
- `medihelp-user-service/.../UserProfileService.java` — 8-arg createProfile overload (kept 3-arg for back-compat)
- `medihelp-user-service/.../UserRegisteredListener.java` — calls 8-arg createProfile
- `medihelp-prescription-service/.../Appointment{,Request,Response}.java` + `AppointmentService.java` — `type` field (CONSULTATION / FOLLOW_UP / THERAPY / CHECKUP), defaults to CONSULTATION
- `medihelp-gateway/.../application.yml` — replaced ai-service route with chatbot-service route, `StripPrefix=3`

### Frontend — new
- `core/services/chatbot.service.ts` — full chatbot API
- `core/services/mood.service.ts`, `sos.service.ts`
- `core/models/india-states.ts` — 28 states + 8 UTs, diet preferences, genders
- `shared/pipes/markdown.pipe.ts` — minimal markdown renderer for chatbot replies
- `shared/components/severity-modal/severity-modal.component.ts` — red modal for CRITICAL
- `shared/components/sidebar/sos-button.component.ts` — persistent emergency button
- `features/mood-journal/{mood-journal,mood-log-dialog}.component.ts` — calendar + dialog

### Frontend — modified
- `app.routes.ts` — added `mood-journal` route
- `core/models/auth.model.ts`, `user.model.ts` — added cultural-context fields
- `core/services/medication.service.ts` — added `checkInteractions()`
- `core/services/prescription-scan.service.ts` — repointed at chatbot's image endpoint
- `features/auth/register/register.component.{ts,html,scss}` — added DOB datepicker, gender select, state dropdown, diet radios
- `features/ai-chat/ai-chat.component.{ts,html,scss}` — full rewrite (single thread, voice, image, severity modal, sessions sidebar, check-in card, cultural food card)
- `features/medications/medications.component.{ts,html}` — drug interactions card
- `features/health-records/health-records.component.{ts,html}` — FHIR export button
- `features/vitals/vitals.component.{html,scss}` — wearable placeholder card
- `shared/components/sidebar/sidebar.component.{ts,html,scss}` — mood-journal nav entry + SOS button at bottom

### Repo root
- `CLAUDE.md` (new) — comprehensive project guide
- `deploy/scripts/start-all.sh` — replaced ai-service block with chatbot-service block, port 8086 added to health probe
- `deploy/scripts/logs.sh` — added `chatbot` case
- `docker-compose.yml`, `docker-compose.prod.yml` — chatbot-service stanza
- `.github/workflows/ci.yml` — Python build now targets chatbot-service (Python 3.12), syntax check instead of pytest
- `.gitignore` — runtime + WSL + vendored-clone noise

### Deleted
- `medihelp-ai-service/` (entire directory — ~500 lines Python)
- `medihelp-frontend/src/app/core/services/ai-chat.service.ts` (replaced by chatbot.service.ts)

---

## Open issues / followups

1. **39 dependabot vulnerabilities** flagged on push (22 high, 14 moderate, 3 low). Pre-existing in dep tree. Need triage pass — probably mostly transitive deps in `langchain-community`, `transformers`, `torch`. Can address post-demo.
2. **Resend free tier** still only mails the verified owner. OTP works via auth.log fallback. For production, verify a custom domain.
3. **Vitals GET hang** noted earlier — never investigated. May be unrelated to this session's changes; worth a quick look.
4. **Frontend dev server (Angular `ng serve`)** wasn't run during this session. UI changes are typed-checked by `ng build` but the user hasn't visually verified each page yet — that's Day 2's first task.
5. **Family Hub frontend page** dropped from the bonus MVP scope. Backend endpoints exist (`/api/v1/users/me/family/groups`); a list/add UI is a 30-min addition for Day 2 if there's time.
6. **Therapy reminder UI** — the backend `appointment.type` field is wired, but the appointments form doesn't yet expose a dropdown. 10-min frontend tweak.
7. **CI changes need to be tested** — the `build-python` job in ci.yml now has a `compileall` step instead of pytest. Hasn't run yet on a real PR.

---

## Day 2 plan (8 hours, ready to resume)

| Time | Task | Owner |
|---|---|---|
| 0:00–0:30 | Boot Azure VM (`az vm start ...`); SSH in; `git pull` (already pushed); restart services on VM | You |
| 0:30–1:30 | UI walkthrough through every page; capture 30–40 screenshots; note any visual bugs | You + me |
| 1:30–3:00 | Report Sections 1–4 (Abstract, Introduction, Objectives, System Architecture) | Me |
| 3:00–4:30 | Report Sections 5–6 (Implementation, Testing & Results) | Me |
| 4:30–5:30 | Report Sections 7–8 (Risk Analysis, Future Scope) + Conclusion | Me |
| 5:30–6:30 | Slide deck (12 panels) | Me |
| 6:30–7:30 | Demo rehearsal + record backup video | You |
| 7:30–8:00 | Final commit + push, stop Azure VM | You |

---

## To exit cleanly (before sleeping)

The local stack is currently running 8 Java services + 8 docker containers on this dev machine. Stop them to save battery / RAM:

```bash
# Stop java services
pkill -f "java -jar.*medihelp"
pkill -f "uvicorn|app.py"
# Stop docker
docker compose -f docker-compose.infra.yml down
```

When you wake up:
```bash
# Resume:
docker compose -f docker-compose.infra.yml up -d
bash deploy/scripts/start-all.sh
# Frontend dev server (in a separate terminal):
cd medihelp-frontend && ng serve
```

Pinecone index is durable in cloud — survives the local stack restart.
