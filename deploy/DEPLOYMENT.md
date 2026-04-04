# MediHelp Deployment Guide

## Cloud Options

| Cloud | VM | RAM | Cost | Duration |
|-------|-----|-----|------|----------|
| **Azure for Students** | Standard_B2s | 4GB | ~$30/month ($100 credit) | ~3 months |
| **Oracle Cloud Free** | A1.Flex | 24GB | $0 forever | Unlimited |

This guide covers **Azure for Students**. The approach works for Oracle too (same Docker Compose).

---

## Prerequisites — Create These Free Accounts

| Service | URL | Purpose | Cost |
|---------|-----|---------|------|
| **Azure for Students** | azure.microsoft.com/free/students | VM hosting | $100 credit, no card |
| **Neon** | neon.tech | 5 PostgreSQL databases | Free (0.5GB each) |
| **MongoDB Atlas** | mongodb.com/atlas | Health records + mood journals | Free (512MB) |
| **Google AI Studio** | ai.google.dev | Gemini API key | Free (1000 req/day) |
| **Resend** | resend.com | Email notifications | Free (3000/month) |
| **GitHub** | github.com | Code + container registry | Free |

---

## Step 1: Create Azure VM

### Via Azure Portal (portal.azure.com)

1. Go to **Virtual Machines > Create**
2. **Basics tab:**
   - Subscription: `Azure for Students`
   - Resource group: Create new → `medihelp-rg`
   - VM name: `medihelp-server`
   - Region: Choose closest (e.g., `Central India`, `East US`)
   - Image: **Ubuntu Server 22.04 LTS**
   - Size: **Standard_B2s** (2 vCPU, 4 GB RAM) — ~$30/month
   - Authentication: **SSH public key**
   - Username: `azureuser`
   - SSH key: Generate new or upload yours
3. **Networking tab:**
   - Public IP: Create new
   - NIC NSG: Basic
   - Public inbound ports: Select **HTTP (80), HTTPS (443), SSH (22)**
   - Click **Add inbound port rule** for: **8080** (Gateway) and **8761** (Eureka)
4. **Review + Create** → wait ~2 minutes

### Or via Azure CLI (if you have `az` installed)

```bash
# Login with student account
az login

# Create resource group
az group create --name medihelp-rg --location centralindia

# Create VM
az vm create \
  --resource-group medihelp-rg \
  --name medihelp-server \
  --image Ubuntu2204 \
  --size Standard_B2s \
  --admin-username azureuser \
  --generate-ssh-keys \
  --public-ip-sku Standard

# Open ports
az vm open-port --resource-group medihelp-rg --name medihelp-server --port 80 --priority 100
az vm open-port --resource-group medihelp-rg --name medihelp-server --port 8080 --priority 101
az vm open-port --resource-group medihelp-rg --name medihelp-server --port 8761 --priority 102

# Get public IP
az vm show -d --resource-group medihelp-rg --name medihelp-server --query publicIps -o tsv
```

### Note the Public IP — you'll need it for everything below.

---

## Step 2: Set Up the VM

```bash
ssh azureuser@YOUR_VM_PUBLIC_IP
```

Run the setup script:
```bash
curl -sSL https://raw.githubusercontent.com/YOUR_GITHUB_USERNAME/medihelp/main/deploy/setup-azure-vm.sh | bash
```

This installs Docker, JDK 21, Node.js 20, creates 4GB swap, and generates `.env` template.

**Important: Log out and back in after this** (for Docker group):
```bash
exit
ssh azureuser@YOUR_VM_PUBLIC_IP
```

---

## Step 3: Set Up Databases

### Neon PostgreSQL (5 databases — all free)

1. Go to **neon.tech** → Sign up
2. Create 5 projects:
   - `medihelp-auth`
   - `medihelp-user`
   - `medihelp-health`
   - `medihelp-prescription`
   - `medihelp-notification`
3. For each: go to **Dashboard** → copy **Connection string**
4. Convert format: `postgres://user:pass@host/db` → `jdbc:postgresql://host/db?sslmode=require`

### MongoDB Atlas (free M0 cluster)

1. Go to **mongodb.com/atlas** → Create free cluster
2. **Database Access**: Create user (e.g., `medihelp` / `yourpassword`)
3. **Network Access**: Add `0.0.0.0/0` (allow from anywhere) or your VM's IP
4. **Connect** → Copy connection string: `mongodb+srv://medihelp:pass@cluster.mongodb.net/medihelp_health`

---

## Step 4: Configure Environment

On the VM:
```bash
nano ~/medihelp/.env
```

Fill in all the values from Step 3. The JWT secret is already auto-generated.

---

## Step 5: Deploy

```bash
# Clone your repo
git clone https://github.com/YOUR_USERNAME/medihelp.git ~/medihelp-src
cd ~/medihelp-src

# Build Java services
mvn clean package -DskipTests -B

# Build Angular frontend
cd medihelp-frontend && npm ci && npx ng build --configuration production && cd ..

# Build Docker images
docker compose -f docker-compose.prod.yml build

# Copy production files
cp docker-compose.prod.yml ~/medihelp/docker-compose.yml

# Start everything
cd ~/medihelp
docker compose up -d
```

Wait ~2 minutes, then verify:
```bash
docker ps                                          # All containers running?
curl http://localhost:8080/actuator/health          # Gateway UP?
curl http://localhost:8761                          # Eureka dashboard?
```

---

## Step 6: Verify End-to-End

```bash
VM_IP=YOUR_VM_PUBLIC_IP

# Register
curl -X POST http://$VM_IP:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@1234","firstName":"Test"}'

# Frontend
echo "Open in browser: http://$VM_IP"
```

---

## Memory Management (4GB VM)

With 4GB + 4GB swap, all services fit. Add JVM limits to `docker-compose.prod.yml` if needed:

```yaml
environment:
  JAVA_OPTS: "-Xmx256m -Xms128m"
```

Expected usage:
| Component | RAM |
|-----------|-----|
| 7 Java services | ~2.1 GB (300MB each with limits) |
| AI Service (Python) | ~150 MB |
| Redis + RabbitMQ | ~250 MB |
| Frontend (Nginx) | ~20 MB |
| OS + Docker | ~500 MB |
| **Total** | **~3.0 GB / 4 GB** (+4GB swap as buffer) |

---

## Cost Tracking

Check your remaining Azure credits:
- Portal → **Cost Management + Billing** → **Azure for Students** → **Overview**
- Or: `az consumption usage list --top 5`

At ~$30/month, your $100 lasts **~3.3 months** — enough for a semester project.

---

## Stopping to Save Credits

When not actively demoing:
```bash
# Stop VM (stops billing for compute)
az vm deallocate --resource-group medihelp-rg --name medihelp-server

# Start VM when needed
az vm start --resource-group medihelp-rg --name medihelp-server
```

**This is important** — deallocating the VM when you're not using it can make your $100 last much longer.

---

## Switching to Oracle Cloud Later

When Oracle Cloud verification works:
1. Create A1.Flex VM (4 OCPUs, 24GB, forever free)
2. Run the same `setup-azure-vm.sh` script (works on any Ubuntu VM)
3. Copy your `.env` file
4. Same `docker compose up -d` command
5. Deallocate the Azure VM to stop charges
