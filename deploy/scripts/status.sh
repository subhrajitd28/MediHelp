#!/bin/bash
# MediHelp - Quick status check
echo "========================================="
echo "  MediHelp - Service Status"
echo "========================================="

# Docker containers
echo ""
echo "Docker Infrastructure:"
docker ps --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  Docker not running"

# Java services
echo ""
echo "Microservices:"
declare -A services=(
    [8761]="Eureka"
    [8080]="Gateway"
    [8081]="Auth"
    [8082]="User"
    [8083]="Health"
    [8084]="Prescription"
    [8085]="Notification"
)
for port in 8761 8080 8081 8082 8083 8084 8085; do
    status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    name=${services[$port]}
    if [ "$status" = "200" ]; then
        echo "  ✓ $name ($port): UP"
    else
        echo "  ✗ $name ($port): DOWN"
    fi
done

# Nginx
echo ""
echo "Frontend (Nginx):"
if systemctl is-active --quiet nginx 2>/dev/null; then
    echo "  ✓ Nginx: Running"
else
    echo "  ✗ Nginx: Not running"
fi

# Memory
echo ""
echo "Memory:"
free -h | awk 'NR==2{printf "  Used: %s / %s (Available: %s)\n", $3, $2, $7}'
echo ""
