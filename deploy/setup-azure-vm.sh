#!/bin/bash
# MediHelp - Azure VM Setup Script
# Run this on a fresh Azure Standard_B2s VM (Ubuntu 22.04+)
# Usage: curl -sSL https://raw.githubusercontent.com/YOUR_REPO/medihelp/main/deploy/setup-azure-vm.sh | bash

set -e

echo "========================================="
echo "  MediHelp - Azure VM Setup"
echo "========================================="

# 1. System updates
echo "[1/7] Updating system..."
sudo apt update && sudo apt upgrade -y

# 2. Install Docker
echo "[2/7] Installing Docker..."
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable docker
sudo usermod -aG docker $USER

# 3. Install JDK 21 + Maven (for building on VM)
echo "[3/7] Installing JDK 21 + Maven..."
sudo apt install -y openjdk-21-jdk-headless maven

# 4. Install Node.js 20 (for Angular build)
echo "[4/7] Installing Node.js 20..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 5. Create swap space (helps with 4GB RAM)
echo "[5/7] Creating 4GB swap..."
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 6. Create app directory
echo "[6/7] Setting up app directory..."
mkdir -p ~/medihelp

# 7. Generate .env template
echo "[7/7] Creating .env template..."
JWT_SECRET=$(openssl rand -base64 48)
cat > ~/medihelp/.env << ENVEOF
# ========== API Keys ==========
GEMINI_API_KEY=
RESEND_API_KEY=

# ========== JWT Secret (auto-generated) ==========
JWT_SECRET=${JWT_SECRET}

# ========== Neon PostgreSQL (https://neon.tech - FREE) ==========
AUTH_DB_URL=jdbc:postgresql://YOUR_NEON_HOST/medihelp_auth?sslmode=require
AUTH_DB_USER=neondb_owner
AUTH_DB_PASSWORD=YOUR_NEON_PASSWORD

USER_DB_URL=jdbc:postgresql://YOUR_NEON_HOST/medihelp_user?sslmode=require
USER_DB_USER=neondb_owner
USER_DB_PASSWORD=YOUR_NEON_PASSWORD

HEALTH_DB_URL=jdbc:postgresql://YOUR_NEON_HOST/medihelp_health?sslmode=require
HEALTH_DB_USER=neondb_owner
HEALTH_DB_PASSWORD=YOUR_NEON_PASSWORD

PRESCRIPTION_DB_URL=jdbc:postgresql://YOUR_NEON_HOST/medihelp_prescription?sslmode=require
PRESCRIPTION_DB_USER=neondb_owner
PRESCRIPTION_DB_PASSWORD=YOUR_NEON_PASSWORD

NOTIFICATION_DB_URL=jdbc:postgresql://YOUR_NEON_HOST/medihelp_notification?sslmode=require
NOTIFICATION_DB_USER=neondb_owner
NOTIFICATION_DB_PASSWORD=YOUR_NEON_PASSWORD

# ========== MongoDB Atlas (https://mongodb.com/atlas - FREE) ==========
MONGODB_URI=mongodb+srv://USER:PASS@cluster.mongodb.net/medihelp_health

# ========== RabbitMQ (local on VM) ==========
RABBITMQ_USER=medihelp
RABBITMQ_PASS=$(openssl rand -hex 16)

# ========== CORS ==========
CORS_ORIGINS=http://YOUR_VM_PUBLIC_IP

# ========== Docker Images ==========
IMAGE_PREFIX=ghcr.io/YOUR_GITHUB_USERNAME/medihelp
ENVEOF

echo ""
echo "========================================="
echo "  Setup complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. LOG OUT and back in (for Docker group to take effect)"
echo "  2. Edit ~/medihelp/.env with your Neon + Atlas credentials"
echo "  3. Clone your repo and build:"
echo "     git clone https://github.com/YOUR_USERNAME/medihelp.git ~/medihelp-src"
echo "     cd ~/medihelp-src"
echo "     mvn clean package -DskipTests -B"
echo "     cd medihelp-frontend && npm ci && npx ng build && cd .."
echo "     docker compose -f docker-compose.prod.yml build"
echo "  4. Copy compose + env:"
echo "     cp docker-compose.prod.yml ~/medihelp/"
echo "     cd ~/medihelp && docker compose up -d"
echo ""
