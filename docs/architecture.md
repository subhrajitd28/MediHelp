# MediHelp Architecture

Diagrams of the deployed system as of `main` (current `1c73bef`). Friend's
HuggingFace cultural-advisor is **not** wired in — `FRIEND_ADVISOR_URL` is
unset, the Groq path serves both structured meal plans and (future) narrative
cultural advice.

---

## 1. System overview

GitHub renders the Mermaid block natively (no images needed in the README/report).

```mermaid
flowchart TB
  classDef ext fill:#fff3e0,stroke:#ff9800
  classDef java fill:#e3f2fd,stroke:#1976d2
  classDef py fill:#e8f5e9,stroke:#43a047
  classDef infra fill:#f3e5f5,stroke:#7b1fa2

  Browser[Angular SPA<br/>:4200]

  subgraph "Gateway layer"
    GW[API Gateway<br/>:8080<br/>JWT validate · inject X-User-Id]:::java
    Eureka[Eureka<br/>:8761]:::java
  end

  subgraph "Application services"
    Auth[Auth<br/>:8081]:::java
    User[User &amp; Profile<br/>:8082]:::java
    Health[Health Tracking<br/>:8083]:::java
    Pres[Prescription<br/>:8084]:::java
    Notif[Notification<br/>:8085]:::java
    Chat[Medical Chatbot<br/>:8086 · Flask + LangChain]:::py
  end

  subgraph "Stateful infra (Docker)"
    PgAuth[(Postgres<br/>auth)]:::infra
    PgUser[(Postgres<br/>user)]:::infra
    PgHealth[(Postgres<br/>health)]:::infra
    PgPres[(Postgres<br/>prescription)]:::infra
    PgNotif[(Postgres<br/>notification)]:::infra
    Mongo[(MongoDB<br/>mood + records)]:::infra
    Redis[(Redis<br/>JWT blacklist + rate-limit)]:::infra
    RMQ[/"RabbitMQ<br/>topic exchange<br/>medihelp.events"/]:::infra
    SQLite[(SQLite<br/>chat sessions + meal plans)]:::infra
  end

  subgraph "External services"
    Groq[Groq<br/>Llama-3.3-70B<br/>+ Whisper + Llama-4-Vision]:::ext
    Pine[Pinecone<br/>RAG · 5,775 chunks]:::ext
    OpenFDA[OpenFDA<br/>drug interactions]:::ext
    Resend[Resend<br/>email/OTP]:::ext
  end

  Browser -- "HTTP + Bearer token" --> GW

  GW --> Auth
  GW --> User
  GW --> Health
  GW --> Pres
  GW --> Notif
  GW --> Chat

  GW <-.heartbeat.-> Eureka
  Auth <-.heartbeat.-> Eureka
  User <-.heartbeat.-> Eureka
  Health <-.heartbeat.-> Eureka
  Pres <-.heartbeat.-> Eureka
  Notif <-.heartbeat.-> Eureka
  Chat <-.heartbeat.-> Eureka

  Auth --> PgAuth
  User --> PgUser
  Health --> PgHealth
  Health --> Mongo
  Pres --> PgPres
  Notif --> PgNotif
  Chat --> SQLite
  Chat --> Pine
  Chat --> Groq

  Auth --> Resend
  Pres --> OpenFDA

  GW --> Redis
  Auth --> Redis

  Auth -. publish .-> RMQ
  Pres -. publish .-> RMQ
  Health -. publish .-> RMQ
  User -. publish .-> RMQ
  RMQ -. consume .-> Notif
  RMQ -. consume .-> User
```

**ASCII fallback** (for terminal viewing):

```
                      ┌──────────────────────────┐
                      │  Angular Browser :4200   │
                      └──────────┬───────────────┘
                                 │ HTTPS + JWT
                                 ▼
                      ┌──────────────────────────┐
                      │  API Gateway :8080       │
                      │  • validate JWT          │
                      │  • inject X-User-Id      │
                      │  • rate-limit (Redis)    │
                      └──────────┬───────────────┘
       ┌──────────┬──────────┬───┼──────────┬──────────┬────────┐
       ▼          ▼          ▼   ▼          ▼          ▼        ▼
   ┌──────┐  ┌──────┐  ┌──────┐ ┌─────┐  ┌──────┐  ┌──────┐ ┌──────┐
   │ Auth │  │ User │  │Health│ │Pres.│  │Notif.│  │Chatbot│ │Eureka│
   │ 8081 │  │ 8082 │  │ 8083 │ │8084 │  │ 8085 │  │ 8086 │ │ 8761 │
   └──┬───┘  └──┬───┘  └──┬───┘ └──┬──┘  └──┬───┘  └──┬───┘ └──────┘
      │         │         │ │      │        │         │
      ▼         ▼         ▼ ▼      ▼        ▼         ▼
   ┌──────┬─────────┬──────┴─┬───────┬───────┬───────────────┐
   │Pg-A  │Pg-User  │Pg-H/Mo │ Pg-P  │ Pg-N  │ SQLite + RAG  │
   └──────┴─────────┴────────┴───────┴───────┴───────────────┘

   Async ─── RabbitMQ topic exchange "medihelp.events" ───
   Auth → user.registered ─→ User · Notif
   Pres → medication.reminder, appointment.reminder ─→ Notif
   Health → vitals.anomaly ─→ Notif
   User  → emergency.sos ─→ Notif

   External
     Groq      ←─── Chatbot (chat/voice/vision)
     Pinecone  ←─── Chatbot (RAG retrieval)
     Resend    ←─── Auth (OTP), Notif (welcome/alerts)
     OpenFDA   ←─── Pres (drug interactions)
```

---

## 2. Authentication & JWT-header injection

The single non-obvious thing: **services never see the JWT**. The gateway
validates it and injects three trusted headers downstream.

```mermaid
sequenceDiagram
  autonumber
  participant U as Browser
  participant G as Gateway
  participant R as Redis
  participant A as Auth :8081
  participant Svc as Any service<br/>(:8082-:8086)

  Note over U,A: Login (one-time)
  U->>G: POST /api/v1/auth/login
  G->>A: forward (public path)
  A-->>U: {accessToken, refreshToken, userId, ...}

  Note over U,Svc: Subsequent calls
  U->>G: GET /api/v1/health/vitals<br/>Authorization: Bearer <jwt>
  G->>G: parse JWT signature
  G->>R: GET blacklist:<jti>
  R-->>G: nil (not blacklisted)
  G->>Svc: GET /api/v1/health/vitals<br/>X-User-Id: 9e4c...<br/>X-User-Email: u@x.local<br/>X-User-Role: USER
  Note over G,Svc: Authorization header is STRIPPED
  Svc->>Svc: read X-User-Id from header
  Svc-->>U: 200 { ... }

  Note over U,A: Token expires (15 min)
  U->>G: ... call ... Bearer <expired>
  G-->>U: 401
  U->>G: POST /api/v1/auth/refresh<br/>{ refreshToken }
  G->>A: forward (public path)
  A-->>U: { accessToken (new), refreshToken (new) }
  U->>G: retry original call with new token<br/>(handled silently by Angular interceptor)
```

**Key invariants:**
- Only `auth-service` knows the JWT secret and parses tokens
- All other services trust `X-User-Id` from the gateway (and only the gateway can write it because the gateway runs *inside* the trusted network)
- Logout = blacklist the JWT's `jti` in Redis. Subsequent calls with same token are rejected even though the signature is still valid

---

## 3. Chatbot RAG pipeline

5 steps per turn, ~5–8 LLM calls. Skips when input is small-talk.

```mermaid
flowchart LR
  In["text / voice / image<br/>input"] --> Smalltalk{"smalltalk<br/>gate"}
  Smalltalk -- "hi · thanks · help" --> Hello["friendly reply<br/>(no LLM cost)"]
  Smalltalk -- "real symptom" --> S0
  S0["Step 0<br/>extract symptoms<br/>JSON via Llama-3.3"] --> S1
  S1["Step 1<br/>disease_chain (RAG)<br/>Pinecone 'general'<br/>k=4 → Llama-3.3"] --> CheckGen{"disease<br/>= 'general'?"}
  CheckGen -- "yes" --> S2["Step 2<br/>fallback_disease<br/>pure-LLM"]
  CheckGen -- "no" --> S3
  S2 --> S3
  S3["Step 3<br/>nutrition_chain (RAG)<br/>Pinecone 'nutrition'<br/>k=5 → 7-row table"] --> S4
  S4["Step 4<br/>severity (3-layer worst-of)<br/>rules + keywords + LLM"] --> CheckCrit{"CRITICAL?"}
  CheckCrit -- "yes" --> Modal["red emergency modal<br/>+ 108 button<br/>(skip home-care)"]
  CheckCrit -- "no" --> S5
  S5["Step 5<br/>home_care_chain<br/>exercise/rest/monitoring"] --> Out
  Modal --> Out
  Out["ChatbotResponse:<br/>disease, severity, nutrition,<br/>home_care, reply"]
```

**Cultural food advice (separate endpoint, called on user click):**

```
POST /api/food-suggestions
  ↓ read nutrition_targets[user, session]
  ↓ check meal_plans cache → return if hit
  ↓ Groq Llama-3.3 with prompt enriched by:
     - disease + restrictions
     - region (Tamil Nadu / Bengal / Punjab / ...)
     - diet_preference (Veg / Non-veg / Vegan / Eggetarian)
     - age + gender
  ↓ parse JSON, persist in meal_plans
  ↓ return structured plan
```

---

## 4. Async event flow (RabbitMQ)

Topic exchange `medihelp.events`. Six event types defined in `medihelp-common`.

```mermaid
flowchart LR
  classDef pub fill:#e3f2fd
  classDef con fill:#fff3e0
  classDef ev fill:#fff,stroke:#888,stroke-dasharray: 5 5

  Auth[Auth]:::pub
  Pres[Prescription]:::pub
  Health[Health]:::pub
  User[User]:::pub

  Auth -- "user.registered<br/>(after OTP verify)" --> X[("medihelp.events<br/>topic exchange")]
  Pres -- "medication.reminder<br/>(scheduler)" --> X
  Pres -- "appointment.reminder<br/>(scheduler)" --> X
  Health -- "vitals.anomaly<br/>(threshold breach)" --> X
  User -- "emergency.sos<br/>(/sos endpoint)" --> X

  X --> Q1["q.profile.user-registered"]
  X --> Q2["q.notification.welcome"]
  X --> Q3["q.notification.medication"]
  X --> Q4["q.notification.appointment"]
  X --> Q5["q.notification.anomaly"]
  X --> Q6["q.notification.sos"]

  Q1 --> UC[User<br/>create UserProfile<br/>with cultural fields]:::con
  Q2 --> NC[Notification<br/>welcome email]:::con
  Q3 --> NC
  Q4 --> NC
  Q5 --> NC
  Q6 --> NC
```

**Event payloads** all carry `userId` so downstream consumers can fetch
caller context if needed. `UserRegisteredEvent` was extended in commit
`0ef2f3e` to include `firstName, lastName, dateOfBirth, gender, state,
dietPreference` so the user-service listener can populate `UserProfile` in
one transaction (no follow-up REST call).

---

## 5. Data residency

Where each user-owned data type lives — useful for the report's data privacy
section.

| Data type | Where | Encryption | Cross-service? |
|---|---|---|---|
| Email + password hash | Postgres-auth | bcrypt | No |
| User profile (name, DOB, gender, state, diet, allergies, conditions) | Postgres-user | TLS in transit | Read-only via `/users/me` |
| Family group + members | Postgres-user | TLS | No |
| Vitals time series | Postgres-health | TLS | Exported to FHIR |
| Mood journal entries | **MongoDB**, AES-256 on `journalText` field | AES-256 | Numeric scale only in FHIR; text never exported |
| Health records (uploaded files) | MongoDB, base64 in document | TLS | FHIR DocumentReference with inline Attachment |
| Prescriptions, medications | Postgres-prescription | TLS | No |
| Drug interaction cache | Postgres-prescription | — | (cached OpenFDA responses) |
| Notifications + preferences | Postgres-notification | TLS | No |
| Chat sessions, messages, meal plans | **SQLite** (chatbot service) | TLS | No |
| RAG vector embeddings | Pinecone cloud | Vendor TLS | No PII |
| JWT blacklist (logout) | Redis | TLS | No |

---

## 6. Service-to-service calls — there are none

Architectural rule: **no Feign clients between services**. All cross-service
data flow goes through one of:

1. **Gateway round-trip** — service A treats service B as an external API,
   calls `/api/v1/...` through the gateway like any client (rare, since
   most cross-service needs are eventual)
2. **RabbitMQ event** — service A publishes, service B subscribes (typical)

This keeps each service's failure domain self-contained: a Postgres outage
on prescription-service doesn't cascade into health-service.

---

## 7. Observability

Self-hosted Prometheus + Grafana (added in `742e95b`):

```
Prometheus :9090  ──── scrapes /actuator/prometheus on :8080-:8085  every 15s
       │
       ▼  (datasource)
Grafana :3000  ───── auto-loads "MediHelp Overview" dashboard
                     (heap by service, p95 latency, request rate, GC pause,
                      thread count, services-up stat)
```

Eureka (:8761) and the Python chatbot (:8086) aren't scraped — Eureka has
its own UI; the chatbot would need `prometheus_flask_exporter` (Phase 2).
