# MediHelp - Personal Health Assistant

> A complete microservice-based healthcare application that provides pre-diagnosis support, post-appointment monitoring, and continuous health management.

**Live Demo**: [http://98.70.34.96](http://98.70.34.96)

---

## Features

- **AI Symptom Triage** - Describe symptoms in plain language, get urgency classification via Google Gemini
- **Prescription OCR** - Upload prescription images, extract medication details via Tesseract
- **Vitals Tracking** - Record and monitor heart rate, blood pressure, temperature, SpO2, blood sugar
- **Anomaly Detection** - Real-time alerts when vitals exceed thresholds (Welford's algorithm)
- **Medication Management** - Track medications, auto-schedule reminders, log adherence
- **Health Score Gamification** - 0-100 score with streaks and badges
- **FHIR R4 Export** - Export health data in industry-standard FHIR format with LOINC codes
- **Mood Journaling** - Daily mood tracking with sleep/exercise correlation
- **Family Health Hub** - Manage dependents with role-based access (Owner/Caregiver/Viewer)
- **Emergency SOS** - One-tap alert sends medical summary to emergency contacts
- **Drug Interaction Checker** - OpenFDA-powered interaction checking with local cache
- **Smart Notifications** - RabbitMQ event-driven notifications for reminders, alerts, appointments

---

## Architecture

```
                     [Angular 17+ SPA]
                           |
                  [API Gateway :8080]
                           |
                  [Eureka Discovery :8761]
                    /    |    |    \
              [Auth]  [User] [Health] [Prescription]
              :8081   :8082  :8083    :8084
                                  \      /
                           [AI Service :8000]
                                  |
                        [Notification :8085]

Infrastructure: PostgreSQL x5 | MongoDB | Redis | RabbitMQ
```

| Service | Tech | Port |
|---------|------|------|
| Frontend | Angular 17 + Material | 80 |
| API Gateway | Spring Cloud Gateway | 8080 |
| Eureka | Spring Cloud Netflix | 8761 |
| Auth | Spring Boot + Security | 8081 |
| User & Profile | Spring Boot | 8082 |
| Health Tracking | Spring Boot + HAPI FHIR | 8083 |
| Prescription | Spring Boot | 8084 |
| AI Service | Python FastAPI | 8000 |
| Notification | Spring Boot | 8085 |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 17, Angular Material, TypeScript |
| Backend | Java 21, Spring Boot 3.3, Spring Cloud 2023.0 |
| AI Service | Python 3.12, FastAPI, Google Gemini, Tesseract OCR |
| Databases | PostgreSQL 16, MongoDB 7, Redis 7 |
| Messaging | RabbitMQ 3 |
| Healthcare | HAPI FHIR R4, LOINC codes, OpenFDA API |
| Deployment | Docker, Azure VM, Nginx |
| CI/CD | GitHub Actions |

---

## Quick Start

### Prerequisites
- JDK 21, Maven 3.8+, Node.js 20+, Docker

### Run locally
```bash
# Infrastructure
docker compose -f docker-compose.infra.yml up -d

# Build
mvn clean package -DskipTests -B

# Start (use the convenience script)
bash deploy/scripts/start-all.sh

# Frontend
cd medihelp-frontend && npm install && ng serve
```

Open http://localhost:4200

---

## Deployment Scripts

| Script | Purpose |
|--------|---------|
| `deploy/scripts/start-all.sh` | Start infrastructure + all services |
| `deploy/scripts/stop-all.sh` | Stop everything |
| `deploy/scripts/restart-all.sh` | Full restart |
| `deploy/scripts/status.sh` | Health check all services |
| `deploy/scripts/logs.sh [service]` | View logs (auth, gateway, errors, otp) |
| `deploy/scripts/azure-stop.ps1` | Stop Azure VM (save credits) |
| `deploy/scripts/azure-start.ps1` | Start Azure VM |

---

## Project Stats

```
244 source files | 65 REST endpoints | 9 microservices
178 Java | 11 Python | 55 Angular | 9 Dockerfiles
22 JPA entities | 6 RabbitMQ listeners | 2 scheduled tasks
```

---

## License

This project is for educational purposes.
