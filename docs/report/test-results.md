# HotelOS Test Scenario Results

Generated: Sun Jun  7 15:50:01 +05 2026

Each scenario invokes the live HotelOS via REST. Responses are captured verbatim.

## TS-01 — Check-in: DOUBLE on floor 3 (no floor 3 → fallback to any floor)

**Request**

```
POST http://localhost:4001/checkin
{"fullName":"Alisher Tursunov","roomType":"DOUBLE","floorPreference":3,"nights":2}
```
**Response**

```
{"guestId":1,"roomNumber":"202","message":"Welcome, Alisher Tursunov. Room 202 on floor 2."}
```
**Expected: assigns longest-clean DOUBLE since floor 3 has none. Among DOUBLE rooms (102/103/202/203), 202 has the oldest lastCleanedAt (2026-05-31).**


## TS-02 — Check-out from 204 after a fresh check-in

**Setup: check in guest to room 204 (SUITE)**

```
POST http://localhost:4001/checkin
{"fullName":"Sardor Karimov","roomType":"SUITE","floorPreference":2,"nights":1}
```
```
{"guestId":2,"roomNumber":"204","message":"Welcome, Sardor Karimov. Room 204 on floor 2."}
```
**Action: check out of 204**

```
POST http://localhost:4001/checkout/204
```
**Response (computed bill)**

```
{"guestName":"Sardor Karimov","roomNumber":"204","nights":1,"roomTotal":260.00,"chargesTotal":0.00,"extras":0.00,"discount":0.00,"grandTotal":260.00,"chargeLines":[]}
```
**Expected: bill = 1 × 260.00, status → DIRTY, room.vacated event published, housekeeping enqueues 204.**

**Housekeeping cleaning queue after the event**

```
["204"]
```

## TS-03 — Mark 204 clean (DIRTY → CLEANING → CLEAN)

**Action: start cleaning**

```
POST http://localhost:4002/start-cleaning/204
```
```
{"roomNumber":"204","status":"CLEANING"}
```
**Action: mark clean**

```
POST http://localhost:4002/mark-clean/204
```
```
{"roomNumber":"204","status":"CLEAN"}
```
**Reception's view of room 204 (should be CLEAN again)**

```
{
  "number": "204",
  "floor": 2,
  "type": "SUITE",
  "status": "CLEAN",
  "metresFromLift": 8,
  "nightlyRate": 260.00,
  "lastCleanedAt": "2026-06-07T10:50:04.018955Z"
}
```

## TS-04 — Room-service order to 105: 2 coffees + 1 sandwich

**Create order**

```
POST http://localhost:4003/orders
{"roomNumber":"105","items":[{"name":"Coffee","quantity":2,"unitPrice":4.50},{"name":"Club Sandwich","quantity":1,"unitPrice":12.00}]}
```
```
{"id":1,"roomNumber":"105","items":[{"name":"Coffee","quantity":2,"unitPrice":4.50},{"name":"Club Sandwich","quantity":1,"unitPrice":12.00}],"total":21.00,"createdAt":"2026-06-07T10:50:05.238035Z","status":"RECEIVED"}
```
**Advance: RECEIVED → PREPARING**

```
{"id":1,"roomNumber":"105","items":[{"name":"Coffee","quantity":2,"unitPrice":4.50},{"name":"Club Sandwich","quantity":1,"unitPrice":12.00}],"total":21.00,"createdAt":"2026-06-07T10:50:05.238035Z","status":"PREPARING"}
```
**Advance: PREPARING → DELIVERING**

```
{"id":1,"roomNumber":"105","items":[{"name":"Coffee","quantity":2,"unitPrice":4.50},{"name":"Club Sandwich","quantity":1,"unitPrice":12.00}],"total":21.00,"createdAt":"2026-06-07T10:50:05.238035Z","status":"DELIVERING"}
```
**Advance: DELIVERING → DELIVERED**

```
{"id":1,"roomNumber":"105","items":[{"name":"Coffee","quantity":2,"unitPrice":4.50},{"name":"Club Sandwich","quantity":1,"unitPrice":12.00}],"total":21.00,"createdAt":"2026-06-07T10:50:05.238035Z","status":"DELIVERED"}
```

## TS-05 — Maintenance: broken shower in 105, urgency CRITICAL

**Report**

```
POST http://localhost:4004/report
{"roomNumber":"105","description":"Broken shower head","urgency":"CRITICAL"}
```
```
{"id":1,"roomNumber":"105","description":"Broken shower head","urgency":"CRITICAL","submittedAt":"2026-06-07T10:50:05.463158Z","assignedTo":"Aziz Karimov","resolved":false,"resolvedAt":null}
```
**Add NORMAL issue too — confirms CRITICAL still at front**

**Priority queue snapshot — CRITICAL must come first**

```
[{"id":1,"roomNumber":"105","description":"Broken shower head","urgency":"CRITICAL","submittedAt":"2026-06-07T10:50:05.463158Z","assignedTo":"Aziz Karimov","resolved":false,"resolvedAt":null},{"id":2,"roomNumber":"203","description":"Air-con noisy","urgency":"NORMAL","submittedAt":"2026-06-07T10:50:05.514138Z","assignedTo":"Dilshod Tursunov","resolved":false,"resolvedAt":null}]
```
**Resolve issue 1**

```
{"id":1,"roomNumber":"105","description":"Broken shower head","urgency":"CRITICAL","submittedAt":"2026-06-07T10:50:05.463158Z","assignedTo":"Aziz Karimov","resolved":true,"resolvedAt":"2026-06-07T10:50:05.549298Z"}
```

## TS-06 — Concurrent check-in: two guests requesting DOUBLE simultaneously

**Fire two check-in requests in parallel**

```
{"guestId":3,"roomNumber":"102","message":"Welcome, Concurrent Guest A. Room 102 on floor 1."}{"guestId":4,"roomNumber":"203","message":"Welcome, Concurrent Guest B. Room 203 on floor 2."}
```
**Expected: two distinct room numbers, no double-booking. Verify by examining the DOUBLE rooms below.**

```
102  DOUBLE  OCCUPIED
103  DOUBLE  CLEAN
202  DOUBLE  OCCUPIED
203  DOUBLE  OCCUPIED
```

## TS-07 — No rooms of the requested type available

**Setup: occupy both SUITES first (104 and 204)**

```
{"guestId":5,"roomNumber":"104","message":"Welcome, Suite Guest One. Room 104 on floor 1."}
```
```
{"guestId":6,"roomNumber":"204","message":"Welcome, Suite Guest Two. Room 204 on floor 2."}
```
**Action: request a third SUITE while none are available**

```
{"suggestion":"Try a different room type or join the wait list.","message":"No clean rooms of type SUITE available right now.","error":"ROOMS_UNAVAILABLE"}
HTTP 409
```
**Expected: 409 with ROOMS_UNAVAILABLE — no crash, system stays up.**


## TS-08 — Invalid input rejected at the boundary

**Empty name**

```
{"message":"fullName must not be blank","error":"VALIDATION_FAILED"}
```
**Invalid room number on /start-cleaning**

```
{"message":"startCleaning.roomNumber: must match \"^[1-2][0-9]{2}$\"","error":"VALIDATION_FAILED"}
```
**Invalid urgency on maintenance report**

```
{"error":"VALIDATION_FAILED","message":"Cannot deserialize value of type `com.hotelos.maintenance.model.Urgency` from String \"NUCLEAR\": not one of the values accepted for Enum class: [HIGH, LOW, CRITICAL, NORMAL]"}
```
**Expected: every invalid input is rejected with VALIDATION_FAILED or similar; system continues serving.**


## Final room state

```
[
  {
    "number": "101",
    "floor": 1,
    "type": "SINGLE",
    "status": "CLEAN",
    "metresFromLift": 5,
    "nightlyRate": 80.00,
    "lastCleanedAt": "2026-06-04T10:49:50.024113Z"
  },
  {
    "number": "102",
    "floor": 1,
    "type": "DOUBLE",
    "status": "OCCUPIED",
    "metresFromLift": 12,
    "nightlyRate": 120.00,
    "lastCleanedAt": "2026-06-02T10:49:50.024113Z"
  },
  {
    "number": "103",
    "floor": 1,
    "type": "DOUBLE",
    "status": "CLEAN",
    "metresFromLift": 18,
    "nightlyRate": 120.00,
    "lastCleanedAt": "2026-06-05T10:49:50.024113Z"
  },
  {
    "number": "104",
    "floor": 1,
    "type": "SUITE",
    "status": "OCCUPIED",
    "metresFromLift": 8,
    "nightlyRate": 250.00,
    "lastCleanedAt": "2026-06-03T10:49:50.024113Z"
  },
  {
    "number": "105",
    "floor": 1,
    "type": "ACCESSIBLE",
    "status": "CLEAN",
    "metresFromLift": 3,
    "nightlyRate": 100.00,
    "lastCleanedAt": "2026-06-06T10:49:50.024113Z"
  },
  {
    "number": "201",
    "floor": 2,
    "type": "SINGLE",
    "status": "CLEAN",
    "metresFromLift": 5,
    "nightlyRate": 85.00,
    "lastCleanedAt": "2026-06-01T10:49:50.024113Z"
  },
  {
    "number": "202",
    "floor": 2,
    "type": "DOUBLE",
    "status": "OCCUPIED",
    "metresFromLift": 12,
    "nightlyRate": 125.00,
    "lastCleanedAt": "2026-05-31T10:49:50.024113Z"
  },
  {
    "number": "203",
    "floor": 2,
    "type": "DOUBLE",
    "status": "OCCUPIED",
    "metresFromLift": 18,
    "nightlyRate": 125.00,
    "lastCleanedAt": "2026-06-03T10:49:50.024113Z"
  },
  {
    "number": "204",
    "floor": 2,
    "type": "SUITE",
    "status": "OCCUPIED",
    "metresFromLift": 8,
    "nightlyRate": 260.00,
    "lastCleanedAt": "2026-06-07T10:50:04.018955Z"
  },
  {
    "number": "205",
    "floor": 2,
    "type": "ACCESSIBLE",
    "status": "CLEAN",
    "metresFromLift": 3,
    "nightlyRate": 105.00,
    "lastCleanedAt": "2026-06-05T10:49:50.024113Z"
  }
]
```
