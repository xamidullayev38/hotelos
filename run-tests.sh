#!/usr/bin/env bash
# Executes the 8 BTEC test scenarios against the running HotelOS.
# Output is appended to docs/report/test-results.md.
set -u
cd "$(dirname "$0")"

OUT=docs/report/test-results.md
mkdir -p docs/report

R=http://localhost:4001
H=http://localhost:4002
S=http://localhost:4003
M=http://localhost:4004

section() { echo "" >> "$OUT"; echo "## $1" >> "$OUT"; echo "" >> "$OUT"; }
step()    { echo "**$1**" >> "$OUT"; echo "" >> "$OUT"; }
code()    { echo '```' >> "$OUT"; echo "$1" >> "$OUT"; echo '```' >> "$OUT"; }

: > "$OUT"
echo "# HotelOS Test Scenario Results" >> "$OUT"
echo "" >> "$OUT"
echo "Generated: $(date)" >> "$OUT"
echo "" >> "$OUT"
echo "Each scenario invokes the live HotelOS via REST. Responses are captured verbatim." >> "$OUT"

# ----------------------------------------------------------------------------
section "TS-01 — Check-in: DOUBLE on floor 3 (no floor 3 → fallback to any floor)"
step "Request"
REQ='{"fullName":"Alisher Tursunov","roomType":"DOUBLE","floorPreference":3,"nights":2}'
code "POST $R/checkin
$REQ"
RES=$(curl -s -X POST $R/checkin -H 'Content-Type: application/json' -d "$REQ")
step "Response"
code "$RES"
step "Expected: assigns longest-clean DOUBLE since floor 3 has none. Among DOUBLE rooms (102/103/202/203), 202 has the oldest lastCleanedAt (2026-05-31)."

# ----------------------------------------------------------------------------
section "TS-02 — Check-out from 204 after a fresh check-in"
step "Setup: check in guest to room 204 (SUITE)"
REQ='{"fullName":"Sardor Karimov","roomType":"SUITE","floorPreference":2,"nights":1}'
code "POST $R/checkin
$REQ"
SETUP=$(curl -s -X POST $R/checkin -H 'Content-Type: application/json' -d "$REQ")
code "$SETUP"

step "Action: check out of 204"
code "POST $R/checkout/204"
RES=$(curl -s -X POST $R/checkout/204)
step "Response (computed bill)"
code "$RES"
step "Expected: bill = 1 × 260.00, status → DIRTY, room.vacated event published, housekeeping enqueues 204."

# Brief wait for broker to fan out
sleep 1
step "Housekeeping cleaning queue after the event"
QUEUE=$(curl -s $H/cleaning-queue)
code "$QUEUE"

# ----------------------------------------------------------------------------
section "TS-03 — Mark 204 clean (DIRTY → CLEANING → CLEAN)"
step "Action: start cleaning"
code "POST $H/start-cleaning/204"
code "$(curl -s -X POST $H/start-cleaning/204)"
sleep 1
step "Action: mark clean"
code "POST $H/mark-clean/204"
code "$(curl -s -X POST $H/mark-clean/204)"
sleep 1
step "Reception's view of room 204 (should be CLEAN again)"
code "$(curl -s $R/rooms | jq '.[] | select(.number=="204")')"

# ----------------------------------------------------------------------------
section "TS-04 — Room-service order to 105: 2 coffees + 1 sandwich"
REQ='{"roomNumber":"105","items":[{"name":"Coffee","quantity":2,"unitPrice":4.50},{"name":"Club Sandwich","quantity":1,"unitPrice":12.00}]}'
step "Create order"
code "POST $S/orders
$REQ"
ORDER=$(curl -s -X POST $S/orders -H 'Content-Type: application/json' -d "$REQ")
code "$ORDER"
ORDER_ID=$(echo "$ORDER" | jq -r '.id')

step "Advance: RECEIVED → PREPARING"
code "$(curl -s -X POST $S/orders/$ORDER_ID/advance)"
step "Advance: PREPARING → DELIVERING"
code "$(curl -s -X POST $S/orders/$ORDER_ID/advance)"
step "Advance: DELIVERING → DELIVERED"
code "$(curl -s -X POST $S/orders/$ORDER_ID/advance)"

# ----------------------------------------------------------------------------
section "TS-05 — Maintenance: broken shower in 105, urgency CRITICAL"
REQ='{"roomNumber":"105","description":"Broken shower head","urgency":"CRITICAL"}'
step "Report"
code "POST $M/report
$REQ"
ISSUE=$(curl -s -X POST $M/report -H 'Content-Type: application/json' -d "$REQ")
code "$ISSUE"
ISSUE_ID=$(echo "$ISSUE" | jq -r '.id')

step "Add NORMAL issue too — confirms CRITICAL still at front"
REQ2='{"roomNumber":"203","description":"Air-con noisy","urgency":"NORMAL"}'
curl -s -X POST $M/report -H 'Content-Type: application/json' -d "$REQ2" > /dev/null
step "Priority queue snapshot — CRITICAL must come first"
code "$(curl -s $M/queue)"

step "Resolve issue $ISSUE_ID"
code "$(curl -s -X POST $M/resolve/$ISSUE_ID)"

# ----------------------------------------------------------------------------
section "TS-06 — Concurrent check-in: two guests requesting DOUBLE simultaneously"
step "Fire two check-in requests in parallel"
(curl -s -X POST $R/checkin -H 'Content-Type: application/json' \
  -d '{"fullName":"Concurrent Guest A","roomType":"DOUBLE","nights":1}' & \
 curl -s -X POST $R/checkin -H 'Content-Type: application/json' \
  -d '{"fullName":"Concurrent Guest B","roomType":"DOUBLE","nights":1}' & wait) > /tmp/concurrent.txt 2>&1
code "$(cat /tmp/concurrent.txt)"
step "Expected: two distinct room numbers, no double-booking. Verify by examining the DOUBLE rooms below."
code "$(curl -s $R/rooms | jq -r '.[] | select(.type=="DOUBLE") | "\(.number)  \(.type)  \(.status)"')"

# ----------------------------------------------------------------------------
section "TS-07 — No rooms of the requested type available"
step "Setup: occupy both SUITES first (104 and 204)"
SUITE1=$(curl -s -X POST $R/checkin -H 'Content-Type: application/json' \
  -d '{"fullName":"Suite Guest One","roomType":"SUITE","nights":1}')
code "$SUITE1"
SUITE2=$(curl -s -X POST $R/checkin -H 'Content-Type: application/json' \
  -d '{"fullName":"Suite Guest Two","roomType":"SUITE","nights":1}')
code "$SUITE2"

step "Action: request a third SUITE while none are available"
RES=$(curl -s -w "\nHTTP %{http_code}" -X POST $R/checkin -H 'Content-Type: application/json' \
  -d '{"fullName":"No-vacancy Guest","roomType":"SUITE","nights":1}')
code "$RES"
step "Expected: 409 with ROOMS_UNAVAILABLE — no crash, system stays up."

# ----------------------------------------------------------------------------
section "TS-08 — Invalid input rejected at the boundary"
step "Empty name"
RES=$(curl -s -X POST $R/checkin -H 'Content-Type: application/json' \
  -d '{"fullName":"","roomType":"SINGLE","nights":1}')
code "$RES"

step "Invalid room number on /start-cleaning"
RES=$(curl -s -X POST $H/start-cleaning/abc)
code "$RES"

step "Invalid urgency on maintenance report"
RES=$(curl -s -X POST $M/report -H 'Content-Type: application/json' \
  -d '{"roomNumber":"101","description":"x","urgency":"NUCLEAR"}')
code "$RES"

step "Expected: every invalid input is rejected with VALIDATION_FAILED or similar; system continues serving."

echo "" >> "$OUT"
echo "## Final room state" >> "$OUT"
echo "" >> "$OUT"
code "$(curl -s $R/rooms | jq .)"

echo "Tests complete — see $OUT"
