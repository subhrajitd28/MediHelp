#!/bin/bash
# MediHelp - View logs
# Usage: ./logs.sh [service-name]
# Example: ./logs.sh auth | ./logs.sh gateway | ./logs.sh all

SERVICE="${1:-all}"

case "$SERVICE" in
    eureka)     tail -100f /tmp/eureka.log ;;
    gateway)    tail -100f /tmp/gateway.log ;;
    auth)       tail -100f /tmp/auth.log ;;
    user)       tail -100f /tmp/user.log ;;
    health)     tail -100f /tmp/health.log ;;
    prescription) tail -100f /tmp/prescription.log ;;
    notification) tail -100f /tmp/notification.log ;;
    errors)
        echo "=== Recent Errors Across All Services ==="
        for log in /tmp/{eureka,gateway,auth,user,health,prescription,notification}.log; do
            name=$(basename $log .log)
            errors=$(grep -c "ERROR" "$log" 2>/dev/null || echo 0)
            echo "  $name: $errors errors"
        done
        echo ""
        echo "=== Last 5 Errors ==="
        grep "ERROR" /tmp/*.log 2>/dev/null | tail -5
        ;;
    otp)
        echo "=== Recent OTPs ==="
        grep "OTP generated" /tmp/auth.log 2>/dev/null | tail -10
        ;;
    all)
        echo "Available: eureka, gateway, auth, user, health, prescription, notification, errors, otp"
        echo "Usage: ./logs.sh <service>"
        ;;
    *)
        echo "Unknown service: $SERVICE"
        echo "Available: eureka, gateway, auth, user, health, prescription, notification, errors, otp"
        ;;
esac
