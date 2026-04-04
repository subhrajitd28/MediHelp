#!/bin/bash
# MediHelp - Stop all services
echo "========================================="
echo "  MediHelp - Stopping All Services"
echo "========================================="

echo "[1/2] Stopping Java services..."
pkill -f "medihelp-" 2>/dev/null && echo "  Java processes stopped." || echo "  No Java processes running."
sleep 2

echo "[2/2] Stopping Docker infrastructure..."
cd "$(dirname $(dirname $(dirname $(realpath $0))))" 2>/dev/null
docker compose -f docker-compose.infra.yml down 2>/dev/null || docker-compose -f docker-compose.infra.yml down 2>/dev/null
echo "  Docker containers stopped."

echo ""
echo "All services stopped."
