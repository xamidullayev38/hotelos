# HotelOS — Real-time Hotel Management System

A microservices-based hotel operations platform built for BTEC HND Unit 4 (Programming).
Four independently-running services communicate through a custom WebSocket message
broker and stream live updates to a browser dashboard.

## Architecture at a glance

```
                  ┌────────────────────┐
                  │  Dashboard (4000)  │  ← browser, live updates
                  └─────────▲──────────┘
                            │ WebSocket
                  ┌─────────┴──────────┐
                  │   Broker (4005)    │  pub/sub
                  └─────────▲──────────┘
                            │
   ┌─────────┬──────────────┼──────────────┬─────────┐
   ▼         ▼              ▼              ▼         ▼
Reception Housekeeping  RoomService   Maintenance
  (4001)     (4002)       (4003)        (4004)
```

- **Reception** — guest check-in/out, runs the **Room Assignment** and **Billing** algorithms
- **Housekeeping** — DIRTY → CLEANING → CLEAN lifecycle
- **Room Service** — FIFO order queue, RECEIVED → PREPARING → DELIVERING → DELIVERED
- **Maintenance** — **Priority Queue** for issues, round-robin technician assignment
- **Broker** — custom WebSocket pub/sub server; no RabbitMQ/Redis needed
- **Dashboard** — vanilla HTML + JS, native WebSocket client

## Requirements

- **Java 17+** (tested with Java 18 and Java 25)
- **Maven 3.6+** (install with `brew install maven` on macOS)
- A free terminal — six processes share one machine

## Build and run

```bash
mvn clean install -DskipTests   # one-time build (~30 sec)
./start.sh                       # launches all six services in the background
```

Wait ~8 seconds for everything to settle, then open:

```
http://localhost:4000            # Dashboard (sign-in token: demo-token)
```

To stop:

```bash
./stop.sh
```

## REST endpoints

| Service | Method | Path | Purpose |
|---------|--------|------|---------|
| Reception | POST | `/checkin` | Body: `{fullName, roomType, floorPreference?, proximityPreference?, nights}` |
| Reception | POST | `/checkout/{roomNumber}` | Returns the computed bill |
| Reception | GET  | `/rooms` | Inventory snapshot |
| Housekeeping | GET  | `/cleaning-queue` | Current queue |
| Housekeeping | POST | `/start-cleaning/{roomNumber}` | DIRTY → CLEANING |
| Housekeeping | POST | `/mark-clean/{roomNumber}` | CLEANING → CLEAN |
| Room Service | GET  | `/orders` | All orders |
| Room Service | POST | `/orders` | Body: `{roomNumber, items:[{name, quantity, unitPrice}]}` |
| Room Service | POST | `/orders/{id}/advance` | Move to next status |
| Maintenance | GET  | `/queue` | Priority-ordered open issues |
| Maintenance | POST | `/report` | Body: `{roomNumber, description, urgency}` |
| Maintenance | POST | `/resolve/{id}` | Close an issue |

## Broker topics

| Topic | Publisher | Subscribers | Payload |
|-------|-----------|-------------|---------|
| `room.vacated` | Reception | Housekeeping, Dashboard | `{roomNumber, vacatedAt}` |
| `room.status_changed` | Reception, Housekeeping | Reception, Dashboard | `{roomNumber, status}` |
| `order.created` | RoomService | Reception, Dashboard | `{orderId, roomNumber, items, total}` |
| `order.status_changed` | RoomService | Dashboard | `{orderId, status}` |
| `maintenance.reported` | Maintenance | Dashboard | `{issueId, roomNumber, urgency, assignedTo}` |
| `maintenance.resolved` | Maintenance | Reception, Dashboard | `{issueId, roomNumber}` |

## Inventory

10 rooms across 2 floors, seeded on first boot of the reception service:

```
Floor 1:  101 SINGLE | 102 DOUBLE | 103 DOUBLE | 104 SUITE | 105 ACCESSIBLE
Floor 2:  201 SINGLE | 202 DOUBLE | 203 DOUBLE | 204 SUITE | 205 ACCESSIBLE
```

## Test scenarios

Run all 8 BTEC test scenarios against the live system:

```bash
./run-tests.sh
```

Results land in `docs/report/test-results.md` with every request/response captured.

## Project layout

```
hotelos/
├── pom.xml                              parent POM
├── start.sh / stop.sh                   process lifecycle
├── run-tests.sh                         executes the 8 BTEC scenarios
├── common/                              shared events, DTOs, BrokerClient
├── broker/                              port 4005 — pub/sub server
├── reception-service/                   port 4001
├── housekeeping-service/                port 4002
├── roomservice-service/                 port 4003
├── maintenance-service/                 port 4004
├── dashboard/                           port 4000 — HTML + JS + WS bridge
└── docs/
    ├── specs/                           design spec
    ├── algorithms/                      flowcharts (Mermaid)
    └── report/                          test-results.md
```

## Security notes

- **Input validation** — every controller DTO uses Jakarta Bean Validation
- **Auth** — the dashboard requires the token `demo-token` (configurable in `dashboard/application.yml`)
- **Data exposure** — event payloads use whitelisted fields, no passport or payment details on the wire
- **XSS** — dashboard escapes all server-sourced text before inserting into the DOM
- **Error handling** — global `@ControllerAdvice` per service catches all exceptions; stack traces never reach the client

## Troubleshooting

- **Port already in use** — `lsof -ti tcp:4001 | xargs kill -9` (repeat for 4000–4005)
- **H2 file lock** — the reception service uses an H2 file under `./data/`. If two instances run, the second fails to start. Solve with `./stop.sh` and `rm -rf data/`.
- **Logs** — every service writes to `logs/<service>.log`. Tail with `tail -f logs/reception.log`.
