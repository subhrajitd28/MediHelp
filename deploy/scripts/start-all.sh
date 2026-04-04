#!/bin/bash
# MediHelp - Start all services
# Usage: ./start-all.sh [path-to-project]

PROJECT_DIR="${1:-$(dirname $(dirname $(dirname $(realpath $0))))}"
cd "$PROJECT_DIR" || exit 1

echo "========================================="
echo "  MediHelp - Starting All Services"
echo "========================================="

# 1. Start Docker infrastructure
echo "[1/4] Starting infrastructure..."
docker compose -f docker-compose.infra.yml up -d 2>/dev/null || docker-compose -f docker-compose.infra.yml up -d
sleep 5

# Wait for infra health
echo "  Waiting for infrastructure to be healthy..."
for i in {1..30}; do
    healthy=$(docker ps --filter "health=healthy" --format "{{.Names}}" 2>/dev/null | wc -l)
    total=$(docker ps --format "{{.Names}}" 2>/dev/null | wc -l)
    if [ "$healthy" -ge "$total" ] && [ "$total" -gt 0 ]; then
        echo "  All $total containers healthy."
        break
    fi
    sleep 2
done

# 2. Start Eureka first
echo "[2/4] Starting Eureka (service discovery)..."
java -Xmx192m -jar medihelp-eureka/target/medihelp-eureka-1.0.0-SNAPSHOT.jar &> /tmp/eureka.log &
echo "  PID: $!"

# Wait for Eureka
echo "  Waiting for Eureka..."
for i in {1..40}; do
    if curl -s http://localhost:8761/actuator/health | grep -q "UP" 2>/dev/null; then
        echo "  Eureka is UP."
        break
    fi
    sleep 2
done

# 3. Start Gateway + all services
echo "[3/4] Starting Gateway + microservices..."
java -Xmx192m -jar medihelp-gateway/target/medihelp-gateway-1.0.0-SNAPSHOT.jar &> /tmp/gateway.log &
echo "  Gateway PID: $!"

java -Xmx256m -jar medihelp-auth-service/target/medihelp-auth-service-1.0.0-SNAPSHOT.jar &> /tmp/auth.log &
echo "  Auth PID: $!"

java -Xmx256m -jar medihelp-user-service/target/medihelp-user-service-1.0.0-SNAPSHOT.jar &> /tmp/user.log &
echo "  User PID: $!"

java -Xmx256m -jar medihelp-health-service/target/medihelp-health-service-1.0.0-SNAPSHOT.jar &> /tmp/health.log &
echo "  Health PID: $!"

java -Xmx256m -jar medihelp-prescription-service/target/medihelp-prescription-service-1.0.0-SNAPSHOT.jar &> /tmp/prescription.log &
echo "  Prescription PID: $!"

java -Xmx256m -jar medihelp-notification-service/target/medihelp-notification-service-1.0.0-SNAPSHOT.jar &> /tmp/notification.log &
echo "  Notification PID: $!"

# 4. Wait and verify
echo "[4/4] Waiting for services to start (40s)..."
sleep 40

echo ""
echo "========================================="
echo "  Service Health Check"
echo "========================================="
declare -A services=(
    [8761]="Eureka"
    [8080]="Gateway"
    [8081]="Auth"
    [8082]="User"
    [8083]="Health"
    [8084]="Prescription"
    [8085]="Notification"
)
all_up=true
for port in 8761 8080 8081 8082 8083 8084 8085; do
    status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    name=${services[$port]}
    if [ "$status" = "200" ]; then
        echo "  ✓ $name ($port): UP"
    else
        echo "  ✗ $name ($port): DOWN (HTTP $status)"
        all_up=false
    fi
done

echo ""
if $all_up; then
    echo "All services are UP! MediHelp is ready."
    echo "  Frontend: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_IP')"
    echo "  Eureka:   http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_IP'):8761"
else
    echo "Some services failed to start. Check logs:"
    echo "  tail -50 /tmp/auth.log"
    echo "  tail -50 /tmp/gateway.log"
fi
echo ""
