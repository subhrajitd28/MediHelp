# Contributing to MediHelp

This is a small group project. The notes below cover the setup that lets every
teammate run the full stack on their own machine, make changes, and propose
them via pull request without stepping on each other's work.

---

## 1. One-time setup

### Get repo access

The repo is private. The owner adds you as a collaborator on GitHub (`Settings
→ Collaborators → Add people`) before you can clone via SSH or HTTPS.

If your name isn't on the collaborators list yet, ping the project lead.

### Clone

```bash
git clone https://github.com/subhrajitd28/MediHelp.git
cd MediHelp
```

(Use SSH if you've set up an SSH key on GitHub: `git@github.com:...`.)

### Toolchain

| Need | Why |
|---|---|
| **JDK 21** | All Java services target 21 |
| **Maven 3.8+** | Multi-module build |
| **Node.js 20** | Angular 17 frontend |
| **Python 3.12** | medihelp-chatbot-service |
| **Tesseract OCR** | `apt install tesseract-ocr` (Linux/WSL) — image OCR layer |
| **Docker + docker compose** | Postgres ×5, Mongo, Redis, RabbitMQ, monitoring stack |

### Service-local secrets

The chatbot reads `medihelp-chatbot-service/.env`. The owner shares the file
out-of-band (via Discord/WhatsApp) — never commit it. Keys you need:

```
PINECONE_API_KEY=pcsk_...
PINECONE_INDEX_NAME=medical-chatbot
GROQ_API_KEY=gsk_...
TESSERACT_CMD=/usr/bin/tesseract        # on Linux
FRIEND_ADVISOR_URL=...                   # optional, Cloudflare tunnel
```

> **CRLF gotcha:** if you edit `.env` on Windows, run
> `sed -i 's/\r$//' .env` before starting services. Hidden carriage
> returns turn into `\r` in HTTP headers and silently break the
> Pinecone client.

### Pinecone index

The medical-chatbot index is already populated (5,775 chunks across `general`
and `nutrition` namespaces). You do **not** need to run `store_index.py`
unless the index is empty.

If you're working on the chatbot and need to rebuild the index from new
PDFs:
```bash
cd medihelp-chatbot-service
python3 store_index.py
```

---

## 2. Run the full stack locally

```bash
# 1. Infra (Postgres ×5, Mongo, Redis, RabbitMQ)
docker compose -f docker-compose.infra.yml up -d

# 2. Build all Java services
mvn clean package -DskipTests -T 1C

# 3. Install chatbot Python deps
pip install --user --break-system-packages -r medihelp-chatbot-service/requirements.txt

# 4. Start every service (Eureka → Gateway → 5 Java services → chatbot)
bash deploy/scripts/start-all.sh

# 5. Frontend (in a separate terminal)
cd medihelp-frontend && npm install && npx ng serve
```

Open `http://localhost:4200`. The smoke-test flow lives in `CLAUDE.md`.

### Optional: monitoring

```bash
docker compose -f docker-compose.monitoring.yml up -d
# Grafana: http://localhost:3000  (admin / admin)
# Prometheus: http://localhost:9090
```

### Stop everything

```bash
bash deploy/scripts/stop-all.sh
docker compose -f docker-compose.infra.yml down
docker compose -f docker-compose.monitoring.yml down
```

---

## 3. Branch + PR workflow

### Branch naming

```
feat/<short-kebab-description>      e.g. feat/whisper-noise-suppression
fix/<short-kebab-description>       e.g. fix/empty-meal-plan-card
chore/<short-kebab-description>     e.g. chore/bump-langchain
docs/<short-kebab-description>      e.g. docs/chatbot-pipeline-diagram
```

### Workflow

```bash
# 1. Always start from latest main
git checkout main && git pull

# 2. Branch
git checkout -b feat/voice-language-detect

# 3. Make changes, run the relevant build:
#    - Frontend changes      → npx ng build
#    - Java service changes  → mvn -pl <module> -am package -DskipTests
#    - Chatbot changes       → python3 -m compileall app.py db.py src/

# 4. Commit
git add <specific-files>     # avoid `git add -A`
git commit -m "Short imperative summary"

# 5. Push + open PR
git push -u origin feat/voice-language-detect
# Then on GitHub: Compare & pull request → request review from project lead
```

### Commit message style

Look at the existing `git log --oneline` for the tone. Examples:

```
Refresh access token transparently on 401
Auto-complete past appointments lazily on list
Expand FHIR export with mood and document resources
Persist chatbot meal plans + fix cultural-advice rendering
```

- One-line summary, imperative voice, ≤ 70 chars
- Optional body explaining *why* (esp. for non-obvious decisions)
- No "WIP" or "fixes" placeholders in main

---

## 4. What service "belongs" to whom

Loose ownership — anyone can fix anything, but PRs land faster when the
owner reviews them:

| Module | Owner |
|---|---|
| medihelp-chatbot-service (Flask + RAG + Groq) | **You — chatbot author** |
| medihelp-auth-service, user-service | Project lead |
| medihelp-health-service | Project lead |
| medihelp-prescription-service, notification-service | Project lead |
| medihelp-frontend (Angular) | Shared |
| Infra / monitoring / CI | Project lead |

For chatbot work specifically, you can usually self-merge to main if the
change is contained to `medihelp-chatbot-service/` and doesn't touch the
gateway route or `app.py:get_current_user`. Anything that breaks the
JSON contract of `/get` / `/get/voice` / `/get/image` should ping the
frontend owner first since the Angular AI-chat page is wired to that
shape (see `medihelp-frontend/src/app/core/services/chatbot.service.ts`).

---

## 5. Don'ts

A few rules the project enforces — please don't break:

- **No Feign clients** between services. Synchronous traffic goes through
  the gateway; cross-service state changes flow over RabbitMQ events
  (`medihelp.events` exchange — see `medihelp-common`).
- **Don't parse JWTs in any service except auth-service.** The gateway
  validates the JWT and injects `X-User-Id`/`X-User-Email`/`X-User-Role`
  headers. Downstream services (including the Python chatbot) trust those
  headers and never look at the `Authorization` header.
- **Don't move DB credentials into the root `.env`.** Each service's
  Postgres URL is in its own `application.yml` with a localhost default;
  the chatbot's secrets live in `medihelp-chatbot-service/.env`.
- **Don't bypass the gateway in code** by calling `lb://service-name`
  from another service. If the chatbot needs user-profile data, call
  `GET /api/v1/users/me` through the gateway like any other client.

For the long version, read `CLAUDE.md` — it has the architecture rules,
endpoint conventions, and smoke-test flow.

---

## 6. Where to find things fast

```
medihelp-chatbot-service/        Python + LangChain + Pinecone + Groq
  app.py                         Flask routes + run_pipeline()
  db.py                          SQLite schema (sessions, messages,
                                 nutrition_targets, user_profile, meal_plans)
  src/prompt.py                  All system prompts for the LLM chains
  src/helper.py                  PDF loading + chunking + embeddings
  store_index.py                 One-off: PDFs → Pinecone

medihelp-frontend/src/app/
  features/ai-chat/              AI chat UI — calls chatbot.service.ts
  core/services/chatbot.service.ts   Strongly-typed wrapper over /api/v1/chatbot/*

medihelp-gateway/src/main/resources/application.yml
                                  Where new gateway routes are added

deploy/monitoring/                Self-hosted Prometheus + Grafana

CLAUDE.md                         Architecture rules + smoke-test
```

---

## 7. Questions

Ping the lead on the project group chat. For chatbot pipeline questions,
the canonical doc is `Medical_Chatbot/Medical_Chatbot_Overview.pdf`.
