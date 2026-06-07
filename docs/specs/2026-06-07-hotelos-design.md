# HotelOS — Design Specification

**Date:** 2026-06-07
**Module:** BTEC HND Digital Technologies — Unit 4: Programming
**Author:** Azamov X
**Status:** Approved — implementation in progress

---

## 1. Overview

HotelOS is a real-time hotel management system that unifies four hotel departments — Reception, Housekeeping, Room Service, and Maintenance — into a single platform built on a microservices architecture. Inter-service communication is handled by a custom WebSocket-based message broker. A browser-based operations dashboard receives live updates over WebSocket.

The system is delivered to the BTEC assessor as a Maven multi-module Java project that runs with a single `./start.sh` command after `mvn clean install`.

## 2. Architectural Goals

- **Isolation**: each microservice owns its domain and its data. Services do not call each other directly.
- **Loose coupling**: all inter-service communication flows through publish/subscribe events on the broker.
- **Live updates**: the dashboard reflects every state change in real time without polling.
- **Run anywhere**: no external services (RabbitMQ, Redis, Docker) required. A clean machine with Java 17+ and Maven can run the system.

## 3. Technology Stack

| Layer | Choice | Justification |
|-------|--------|---------------|
| Language | Java 17 | BTEC is language-agnostic; Java is industry-standard for enterprise systems and demonstrates OOP cleanly |
| Build | Maven multi-module | Standard layout; one `mvn install` builds every module |
| Web framework | Spring Boot 3 | Each microservice is an independent runnable JAR with embedded Tomcat |
| Broker | Custom WebSocket pub/sub (Spring WebSocket) | Zero external dependencies; demonstrates broker pattern without coupling to RabbitMQ specifics |
| Real-time | Spring WebSocket | Same library used for broker and dashboard updates |
| Persistence | H2 (file-mode) + Spring Data JPA | No installation required; JPA entities double as OOP demonstration |
| Validation | Jakarta Bean Validation | Satisfies security Task 3.2 — input validation at boundary |
| Dashboard | Vanilla HTML + JS + native WebSocket | No build step; opens directly in browser |

## 4. Module Layout

```
hotelos/
├── pom.xml                              parent POM
├── start.sh                             launches all six processes
├── common/                              shared events, DTOs, constants
├── broker/                              port 4005 — WebSocket pub/sub
├── reception-service/                   port 4001
├── housekeeping-service/                port 4002
├── roomservice-service/                 port 4003
├── maintenance-service/                 port 4004
└── dashboard/                           port 4000 — HTML + WS bridge
```

## 5. Services and Responsibilities

### 5.1 Reception (4001)
- `POST /checkin` — runs the **Room Assignment Algorithm**, marks room OCCUPIED, persists guest
- `POST /checkout/{roomNumber}` — runs the **Billing Algorithm**, publishes `room.vacated`
- `GET /rooms` — returns inventory snapshot
- Subscribes to `order.created` to charge orders to room bill, and `maintenance.resolved`

### 5.2 Housekeeping (4002)
- `GET /cleaning-queue` — current queue
- `POST /start-cleaning/{roomNumber}` — DIRTY → CLEANING, publishes `room.status_changed`
- `POST /mark-clean/{roomNumber}` — CLEANING → CLEAN, publishes `room.status_changed`
- Subscribes to `room.vacated` and enqueues the room

### 5.3 Room Service (4003)
- `POST /orders` — creates order in RECEIVED state, publishes `order.created`
- `POST /orders/{id}/advance` — RECEIVED → PREPARING → DELIVERING → DELIVERED, publishes `order.status_changed`
- FIFO queue ordering

### 5.4 Maintenance (4004)
- `POST /report` — creates issue, **Priority Queue Algorithm** assigns it to next technician, publishes `maintenance.reported`
- `POST /resolve/{id}` — closes issue, publishes `maintenance.resolved`
- `GET /queue` — current priority-ordered issues

## 6. Events on the Broker

| Event | Publisher | Subscribers | Payload |
|-------|-----------|-------------|---------|
| `room.vacated` | Reception | Housekeeping, Dashboard | `{roomNumber, vacatedAt}` |
| `room.status_changed` | Housekeeping, Reception | Reception, Dashboard | `{roomNumber, status}` |
| `order.created` | RoomService | Reception, Dashboard | `{orderId, roomNumber, items, total}` |
| `order.status_changed` | RoomService | Dashboard | `{orderId, status}` |
| `maintenance.reported` | Maintenance | Dashboard | `{issueId, roomNumber, urgency, assignedTo}` |
| `maintenance.resolved` | Maintenance | Reception, Dashboard | `{issueId, roomNumber}` |

The broker is a generic pub/sub server. Clients send `{"action":"subscribe","topic":"room.vacated"}` to subscribe and `{"action":"publish","topic":"room.vacated","payload":{...}}` to publish. The broker fan-outs each `publish` to every subscriber of that topic.

## 7. Algorithms

### 7.1 Room Assignment (Reception)
Multi-criteria filter + sort:
1. Filter: `type == requested` AND `status == CLEAN`
2. If `floorPreference` set: prefer that floor (fallback to any floor)
3. Sort by `lastCleanedAt` ASC (oldest-clean first — fair rotation)
4. If tie and `proximityPreference` set (LIFT or STAIRS): use proximity score
5. Return first; if empty, return `ROOMS_UNAVAILABLE`

### 7.2 Billing (Reception)
```
total = (nights × room.nightlyRate)
      + Σ orderCharges
      + Σ extras (minibar, late checkout)
      − discount
```
Edge cases: early checkout uses actual nights; zero orders → only room rate; discount cannot exceed subtotal.

### 7.3 Maintenance Priority Queue
`java.util.PriorityQueue<MaintenanceIssue>` with comparator:
1. Urgency ordinal (CRITICAL=0 first)
2. Tie: `submittedAt` ASC (FIFO within same urgency)

## 8. Data Structures

| Data | Structure | Why |
|------|-----------|-----|
| Room inventory | `List<Room>` + JPA | Bounded (10 rooms in demo); indexed by room number |
| Guests | JPA `Map`-like via `findById` | Keyed lookup by guest ID |
| Cleaning queue | `LinkedList<String>` | FIFO of room numbers |
| Order queue | `LinkedList<Order>` | FIFO of orders |
| Maintenance issues | `PriorityQueue<MaintenanceIssue>` | Heap-based priority retrieval — O(log n) |

## 9. Security (Task 3.2)

| Concern | Mitigation |
|---------|------------|
| Input validation | Jakarta Bean Validation on every controller DTO (`@NotNull`, `@Min`, `@Max`, `@Pattern`) |
| Authentication | Dashboard requires header `Authorization: Bearer demo-token`; rejected with 401 otherwise |
| Data exposure | Event payloads use whitelisted fields; no `paymentDetails`, `passportNumber`, full guest record over the wire |
| Error handling | Global `@ControllerAdvice` catches all exceptions; returns sanitised JSON; stack trace stays in server log |

## 10. Test Scenarios (Task 3.4)

Eight scenarios TS-01 through TS-08 will be executed via REST calls and the dashboard. Each will produce documented output captured to `docs/report/test-results.md`.

## 11. Build and Run

```bash
cd hotelos
mvn clean install -DskipTests   # builds every module into target/
./start.sh                       # launches all six JARs in background
# Open http://localhost:4000 in browser
./stop.sh                        # kills them
```

## 12. Out of Scope

- Real payment processing (charges are recorded but not transacted)
- 120 rooms — the demo uses 10 rooms across 2 floors per the assignment guidance
- Production hardening (TLS, multi-tenant auth, distributed tracing)
- UI polish — dashboard is functional, not designed
