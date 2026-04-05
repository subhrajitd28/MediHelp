#!/bin/bash
PROJECT_DIR="${1:-$(dirname $(dirname $(dirname $(realpath $0))))}"
cd "$PROJECT_DIR" || exit 1

# Load cloud credentials
set -a; source "$PROJECT_DIR/.env" 2>/dev/null; set +a

echo "========================================="
echo "  MediHelp - Starting All Services"
echo "========================================="

echo "[1/4] Starting infrastructure..."
docker compose -f docker-compose.infra.yml up -d 2>/dev/null
sleep 5
echo "  Infrastructure started."

echo "[2/4] Starting Eureka..."
java -Xmx192m -jar medihelp-eureka/target/medihelp-eureka-1.0.0-SNAPSHOT.jar &> /tmp/eureka.log &
echo "  PID: $!"
for i in {1..40}; do curl -s http://localhost:8761/actuator/health | grep -q "UP" 2>/dev/null && echo "  Eureka is UP." && break; sleep 2; done

echo "[3/4] Starting services..."
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

echo "[4/4] Waiting (50s)..."
sleep 50

echo ""
echo "========================================="
echo "  Service Health Check"
echo "========================================="
declare -A services=([8761]="Eureka" [8080]="Gateway" [8081]="Auth" [8082]="User" [8083]="Health" [8084]="Prescription" [8085]="Notification")
for port in 8761 8080 8081 8082 8083 8084 8085; do
    status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    name=${services[$port]}
    if [ "$status" = "200" ]; then echo "  ✓ $name ($port): UP"; else echo "  ✗ $name ($port): DOWN"; fi
done
