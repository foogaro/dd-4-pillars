# Hotel Booking Microservices (OpenTelemetry + Datadog)

Java 25 / Spring Boot 4 microservices for hotel room booking. Telemetry (metrics, logs, traces) is exported via OTLP to the Datadog Agent. Persistence is Redis 8 with **RedisJSON** (`JSON.SET` / `JSON.GET` / `JSON.DEL`) for hotel, room, booking, stay, and workflow documents. Secondary indexes use Redis Sets; room locks use Redis Strings.

## Services

| Service | Port | Responsibility |
|---------|------|----------------|
| `hotel-service` | 8081 | Hotel CRUD |
| `room-service` | 8082 | Room CRUD, reserve/release locks |
| `booking-service` | 8083 | Booking + simulated payment (`ACCEPT` / `DECLINE`) |
| `checkin-service` | 8084 | Check-in / check-out |
| `workflow-service` | 8085 | End-to-end book & cancel orchestration |
| Redis | 6379 | Persistence |
| Datadog Agent | 4317/4318 | OTLP ingest (gRPC/HTTP) |

Anonymous sessions use the `X-Session-Id` header (auto-generated when missing).

## API docs (OpenAPI / Swagger UI)

Each service exposes OpenAPI 3 via [springdoc-openapi](https://springdoc.org/):

| Service | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| hotel-service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| room-service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| booking-service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| checkin-service | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |
| workflow-service | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |

## Prerequisites

- Java 25, Maven 3.9+
- Docker + Docker Compose
- Datadog API key (`DD_API_KEY`) for Agent → Datadog

## Run with Docker Compose

```bash
export DD_API_KEY=your_datadog_api_key
export DD_SITE=datadoghq.eu   # optional
docker compose up --build
```

## Local development

```bash
docker compose up -d redis datadog-agent
mvn -B -DskipTests package
# start each service, or run from your IDE
```

## Example flow

```bash
SESSION=$(uuidgen)

# 1. Create hotel
HOTEL=$(curl -s -X POST http://localhost:8081/api/hotels \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION" \
  -d '{"name":"Grand Foogaro","city":"Milan","country":"IT","address":"Via Roma 1","stars":5}')
HOTEL_ID=$(echo "$HOTEL" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

# 2. Create room
ROOM=$(curl -s -X POST http://localhost:8082/api/rooms \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION" \
  -d "{\"hotelId\":\"$HOTEL_ID\",\"number\":\"101\",\"type\":\"SUITE\",\"capacity\":2,\"pricePerNight\":250.00}")
ROOM_ID=$(echo "$ROOM" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

# 3. Book via workflow (force payment accept)
WF=$(curl -s -X POST http://localhost:8085/api/workflows/book \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION" \
  -d "{\"hotelId\":\"$HOTEL_ID\",\"roomId\":\"$ROOM_ID\",\"checkIn\":\"2026-11-01\",\"checkOut\":\"2026-11-03\",\"paymentOutcome\":\"ACCEPT\"}")
echo "$WF" | python3 -m json.tool
WF_ID=$(echo "$WF" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
BOOKING_ID=$(echo "$WF" | python3 -c "import sys,json; print(json.load(sys.stdin)['bookingId'])")

# 4. Check-in / check-out
curl -s -X POST http://localhost:8084/api/stays/check-in \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION" \
  -d "{\"bookingId\":\"$BOOKING_ID\"}" | python3 -m json.tool

curl -s -X POST http://localhost:8084/api/stays/check-out \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION" \
  -d "{\"bookingId\":\"$BOOKING_ID\"}" | python3 -m json.tool

# 5. Cancel another confirmed booking (create a second one first), or cancel before check-in:
# curl -s -X POST http://localhost:8085/api/workflows/$WF_ID/cancel -H "X-Session-Id: $SESSION"
```

To simulate a declined payment, set `"paymentOutcome":"DECLINE"` on the book request. Omit it for a random outcome.

## Observability

Each service emits:

- **Metrics** — business counters (`hotel.created`, `booking.payment.accepted`, `workflow.book.confirmed`, …) plus JVM/HTTP via Micrometer → OTLP
- **Traces** — Micrometer Tracing / OpenTelemetry with W3C propagation across RestClient calls
- **Logs** — JSON to stdout (trace/span/session/workflow MDC) and OpenTelemetry Logback appender → OTLP

Agent OTLP endpoints: `http://datadog-agent:4318` (HTTP), `4317` (gRPC).

### If services do not appear in Datadog APM

1. Confirm `DD_API_KEY` and `DD_SITE` match the org you are viewing (US `datadoghq.com` vs EU `datadoghq.eu`).
2. Agent 7.61+ needs `HOST_PROC=/proc` in Compose (already set) so the OTLP pipeline can start on `:4318`.
3. Look under **APM → Services** with env `dev`, and under **Infrastructure → Hosts** for `dd-4-pillars-agent`.
4. Generate traffic (see example flow above); traces can take 1–2 minutes to show.

## Workflow states

`STARTED` → `ROOM_RESERVED` → `PAYMENT_IN_PROGRESS` → `PAYMENT_ACCEPTED` → `CONFIRMED`

On decline: `PAYMENT_DECLINED` → `COMPENSATING` → `FAILED`

Cancel: `CONFIRMED` → `CANCEL_REQUESTED` → `ROOM_RELEASED` → `CANCELLED`
