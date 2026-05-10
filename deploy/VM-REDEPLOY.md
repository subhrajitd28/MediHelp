# VM Redeploy Guide

How to bring the Azure VM back online with the current `main` branch
(includes the chatbot service, all UI fixes, and the self-hosted monitoring
stack).

The VM has been deallocated since 2026-04-08; everything below assumes you
start from a fresh boot.

---

## Pre-flight on your laptop

1. **Push everything you want deployed.** The VM only pulls; it never pushes.
   ```bash
   git status        # should be clean
   git push origin main
   ```

2. **MongoDB Atlas paused itself** after 30 days of inactivity. Pick one:
   - **Resume Atlas** — log in to https://cloud.mongodb.com → MediHelp-Cluster → "Resume". Free tier resumes in ~3 minutes. Pause cycle restarts.
   - **Switch to local Mongo on the VM** (recommended — no more pause cycle). Done by NOT setting `MONGODB_URI` in the env, so the health-service falls back to its `mongodb://localhost:27017/medihelp_health` default. Mongo runs as a Docker container on the VM.

   The rest of this guide assumes **local Mongo on the VM** since that's what survives Atlas pauses.

---

## 1. Wake the VM

From your laptop's PowerShell:

```powershell
az vm start --resource-group medihelp-rg --name medihelp-server
az vm show -d -g medihelp-rg -n medihelp-server --query publicIps -o tsv
# (note the IP)
```

SSH in (the deploy key the other claude session set up is read-only — that's fine for git pull, but you'll be SSH-ing to the VM as `azureuser` with your own key):

```bash
ssh azureuser@<VM_IP>
```

---

## 2. Pull the new code

```bash
cd ~/medihelp
git pull origin main           # uses the read-only deploy key
git log --oneline -5           # sanity check the latest commits are there
```

You should see at least these commits:

```
cfcc485  Add quick-log vital widget on dashboard
3492abd  Persist chatbot meal plans + fix cultural-advice rendering
e639692  Expand FHIR export with mood and document resources
ea3617b  Auto-complete past appointments lazily on list
f8b5088  Refresh access token transparently on 401
126e89f  Family Hub UI + appointment type/profile/mood polish + chatbot smalltalk gate
8f1cab6  Remove medihelp-ai-service and finish chatbot-service cutover
```

If the deploy key's permissions changed, the prior session documented the
SSH-deploy-key setup in `2026-05-10-day1-session-summary.md`.

---

## 3. Install / upgrade VM toolchain

These are idempotent — safe to re-run on every redeploy.

```bash
# Tesseract (OCR layer in the chatbot's image pipeline)
sudo apt-get update
sudo apt-get install -y tesseract-ocr

# Python deps for chatbot
pip install --user --break-system-packages -r ~/medihelp/medihelp-chatbot-service/requirements.txt
pip install --user --break-system-packages "numpy<2"   # torch needs numpy 1.x

# Java + Maven should already be installed from the prior deploy.
java -version    # expect 21
mvn -version     # expect 3.8+
```

---

## 4. Service-local secrets

```bash
# Chatbot needs Pinecone + Groq keys.
nano ~/medihelp/medihelp-chatbot-service/.env
```

Paste (use YOUR keys — not teammate's):

```
PINECONE_API_KEY=pcsk_xxxxxxxxxxxxxxxxxxxxxxxxxx
PINECONE_INDEX_NAME=medical-chatbot
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxx
TESSERACT_CMD=/usr/bin/tesseract
SECRET_KEY=<random-32-char-string>
```

Then strip CRLF in case you pasted from Windows:

```bash
sed -i 's/\r$//' ~/medihelp/medihelp-chatbot-service/.env
```

---

## 5. Bring up infra (Postgres ×5, Mongo, Redis, RabbitMQ)

```bash
cd ~/medihelp
docker compose -f docker-compose.infra.yml up -d
docker ps                  # 8 containers, all "healthy" within ~30s
```

This swaps Atlas for **local Mongo on the VM** — kills the auto-pause problem.

---

## 6. Build + start all services

```bash
cd ~/medihelp
mvn clean package -DskipTests -T 1C
bash deploy/scripts/start-all.sh
```

After ~60 seconds:

```bash
bash deploy/scripts/status.sh        # all 8 services should show ✓
curl http://localhost:8761/eureka/apps | grep '<name>'    # 7 services registered
```

---

## 7. Self-hosted monitoring (replaces Grafana Cloud)

```bash
cd ~/medihelp
docker compose -f docker-compose.monitoring.yml up -d
```

Verify:

```bash
curl http://localhost:9090/-/ready                       # Prometheus: "Prometheus Server is Ready."
curl http://localhost:3000/api/health                    # Grafana:    {"version":"...","status":"ok"}
```

The `MediHelp Overview` dashboard is auto-provisioned — no manual import needed.

---

## 8. Open ports in Azure NSG

The previous deploy opened 80, 8080, 8761. Add these:

| Port | Purpose | Priority suggestion |
|---|---|---|
| **8086** | Chatbot service (in case you want to hit it directly) | 330 |
| **3000** | Grafana | 340 |
| **9090** | Prometheus | 350 |

Azure portal → Virtual Machines → medihelp-server → Networking → Inbound port rules → Add.

For demo, you only really need 80 (frontend) + 3000 (Grafana). The rest are for debugging.

---

## 9. Smoke test from your laptop

```bash
VM=<VM_IP>

# Frontend reachable?
curl -sf -o /dev/null -w "Frontend: %{http_code}\n" http://$VM/

# Gateway?
curl -sf http://$VM:8080/actuator/health | head -c 60; echo

# Eureka registry has 7 services?
curl -sf http://$VM:8761/eureka/apps | grep -o '<name>[A-Z-]*</name>' | sort -u

# Grafana?
curl -sf -o /dev/null -w "Grafana: %{http_code}\n" http://$VM:3000/api/health
```

Open `http://$VM` in a browser, register a fresh user with the new
DOB/state/diet form, log in, hit the AI chat with a real symptom, and
walk through every page.

---

## 10. Test data + demo prep

```bash
# Get the latest OTP from the auth log
ssh azureuser@<VM_IP> "bash ~/medihelp/deploy/scripts/logs.sh otp"

# Tail chatbot for any errors during testing
ssh azureuser@<VM_IP> "bash ~/medihelp/deploy/scripts/logs.sh chatbot"
```

Demo-ready URLs:

| URL | What it shows |
|---|---|
| `http://<VM_IP>/` | Angular frontend — full app |
| `http://<VM_IP>:8761` | Eureka — proves all services discoverable |
| `http://<VM_IP>:3000` | Grafana — live JVM heap, CPU, p95 latency, request rate |
| `http://<VM_IP>:9090` | Prometheus — raw metrics + scrape targets list |

---

## 11. When you're done — save credits

```powershell
az vm deallocate --resource-group medihelp-rg --name medihelp-server
```

Pinecone (cloud index) keeps the embeddings hot regardless. Local Mongo's
data persists in the docker volume so it's there next boot — no Atlas pause
to deal with.

---

## Common gotchas

- **`8084` 503 from gateway** — prescription service didn't fully start. Check `bash deploy/scripts/logs.sh prescription | tail`. Usually a Postgres connection retry that times out.
- **Chatbot returns mock responses** — `.env` not loaded or wrong keys. Check `tail /tmp/chatbot.log` for "Failed to initialize Gemini client" or "Pinecone API key missing".
- **Frontend shows "Service Unavailable"** for AI chat — chatbot service didn't register with Eureka. Wait 30s after start, retry.
- **Grafana dashboard empty** — Java services not exposing `/actuator/prometheus`. Check `curl localhost:8081/actuator/prometheus` returns metric data.
