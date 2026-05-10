#!/bin/bash
# End-to-end smoke test that exercises every backend store the app touches.
# Designed for the VM after the cloud→local DB migration so you can confirm
# Postgres-Auth, Postgres-User, Postgres-Health, MongoDB, and the
# Pinecone+Groq chatbot are all serving requests.
#
# Usage:  bash deploy/scripts/smoke-test.sh
# Exit:   0 if every check passes, 1 otherwise.

set -u

GATEWAY="${GATEWAY:-http://localhost:8080}"
AUTH_LOG="${AUTH_LOG:-/tmp/auth.log}"
EMAIL="smoke$(date +%s)@medihelp.local"
PW="SmokeTest12345"

ok()   { printf "  ✓ %-32s %s\n" "$1" "$2"; }
fail() { printf "  ✗ %-32s %s\n" "$1" "$2"; FAILED=1; }

FAILED=0
echo ""
echo "=== Smoke test against $GATEWAY ==="

# 1. Register
RESP=$(curl -s -X POST "$GATEWAY/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PW\",\"firstName\":\"L\",\"lastName\":\"T\",\"dateOfBirth\":\"1995-06-15\",\"gender\":\"Male\",\"state\":\"Tamil Nadu\",\"dietPreference\":\"Vegetarian\"}")
echo "$RESP" | grep -q '"success":true' && ok "register"  "" || { fail "register" "$RESP"; exit 1; }

sleep 2

# 2. OTP from log
OTP=$(grep "OTP generated for $EMAIL" "$AUTH_LOG" 2>/dev/null | tail -1 | grep -oE '[0-9]{6}' | tail -1)
[ -n "$OTP" ] && ok "OTP captured" "$OTP" || { fail "OTP captured" "no OTP in $AUTH_LOG"; exit 1; }

# 3. Verify OTP
RESP=$(curl -s -X POST "$GATEWAY/api/v1/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"otp\":\"$OTP\"}")
echo "$RESP" | grep -q '"success":true' && ok "verify-otp" "" || { fail "verify-otp" "$RESP"; exit 1; }

# 4. Login → JWT
LOGIN=$(curl -s -X POST "$GATEWAY/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PW\"}")
TOKEN=$(echo "$LOGIN" | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])' 2>/dev/null || true)
[ ${#TOKEN} -gt 100 ] && ok "login (Postgres-Auth)" "${#TOKEN}-char token" || { fail "login" "$LOGIN"; exit 1; }

# 5. /users/me — Postgres-User profile (populated via RabbitMQ listener)
RESP=$(curl -s -H "Authorization: Bearer $TOKEN" "$GATEWAY/api/v1/users/me")
STATE=$(echo "$RESP" | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"].get("state",""))' 2>/dev/null || true)
[ "$STATE" = "Tamil Nadu" ] && ok "Postgres-User /users/me" "state='$STATE'" || fail "/users/me" "$RESP"

# 6. POST vital — Postgres-Health
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"HEART_RATE","value":72,"unit":"bpm"}' \
  "$GATEWAY/api/v1/health/vitals")
[ "$HTTP" = "200" ] && ok "Postgres-Health POST vital" "" || fail "POST vital" "HTTP $HTTP"

# 7. POST mood — MongoDB (the critical one for cloud-to-local migration)
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"mood":4,"sleepHours":7,"exerciseMinutes":30}' \
  "$GATEWAY/api/v1/health/mood")
[ "$HTTP" = "200" ] && ok "MongoDB POST mood" "local Mongo serving" || fail "POST mood (MongoDB)" "HTTP $HTTP — Mongo unreachable?"

# 8. Chatbot text — Pinecone RAG + Groq
RESP=$(curl -s --max-time 30 -X POST -H "Authorization: Bearer $TOKEN" \
  -F "msg=Hi there" "$GATEWAY/api/v1/chatbot/get")
echo "$RESP" | grep -q '"reply"' && ok "Chatbot (Pinecone + Groq)" "" || fail "Chatbot" "$(echo "$RESP" | head -c 120)"

echo ""
if [ $FAILED -eq 0 ]; then
  echo "=== All checks passed ==="
  exit 0
else
  echo "=== Some checks failed (see above) ==="
  exit 1
fi
