#!/bin/bash
# MediHelp - Oracle Cloud VM Setup Script
# Run this on a fresh Oracle Cloud Always Free ARM instance (Ubuntu 22.04+)
# Usage: curl -sSL https://raw.githubusercontent.com/YOUR_REPO/medihelp/main/deploy/setup-oci-vm.sh | bash

set -e

echo "========================================="
echo "  MediHelp - Oracle Cloud VM Setup"
echo "========================================="

# 1. System updates
echo "[1/6] Updating system..."
sudo apt update && sudo apt upgrade -y

# 2. Install Docker
echo "[2/6] Installing Docker..."
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER

# 3. Install JDK 21 (for building locally if needed)
echo "[3/6] Installing JDK 21..."
sudo apt install -y openjdk-21-jdk-headless

# 4. Create app directory
echo "[4/6] Setting up app directory..."
mkdir -p ~/medihelp
cd ~/medihelp

# 5. Create .env template
echo "[5/6] Creating .env template..."
cat > .env << 'ENVEOF'
# ========== API Keys ==========
GEMINI_API_KEY=
RESEND_API_KEY=

# ========== JWT Secret ==========
JWT_SECRET=change-this-to-a-random-256-bit-secret-use-openssl-rand-base64-32

# ========== Neon PostgreSQL ==========
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

# ========== MongoDB Atlas ==========
MONGODB_URI=mongodb+srv://USER:PASS@cluster.mongodb.net/medihelp_health

# ========== RabbitMQ (local on VM) ==========
RABBITMQ_USER=medihelp
RABBITMQ_PASS=CHANGE_THIS

# ========== CORS ==========
CORS_ORIGINS=http://YOUR_VM_PUBLIC_IP

# ========== Docker Images ==========
IMAGE_PREFIX=ghcr.io/YOUR_GITHUB_USERNAME/medihelp
ENVEOF

# 6. Open firewall ports
echo "[6/6] Opening firewall ports..."
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 8761 -j ACCEPT
# Save iptables rules
sudo apt install -y iptables-persistent
sudo netfilter-persistent save

echo ""
echo "========================================="
echo "  Setup complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Log out and back in (for Docker group)"
echo "  2. Edit ~/medihelp/.env with your credentials"
echo "  3. Copy docker-compose.prod.yml to ~/medihelp/"
echo "  4. Run: cd ~/medihelp && docker compose -f docker-compose.prod.yml up -d"
echo ""
echo "Or use GitHub Actions deploy workflow for automated deployment."
echo ""
