# VROS optimization with Kafka and a separate worker

This implementation adds an asynchronous vehicle-routing (VRP) pipeline to VROS:

```
REST ──► VROS API ──requests──► Kafka ──► OptimizationWorker (greedy solver)
              ▲                                  │
              └──── acks / progress / results ◄──┘
                         │
                         └──► STOMP /topic/optimization/{jobId}
```

VROS owns job state in PostgreSQL. The worker is stateless: it receives a complete problem
snapshot, solves it on a bounded executor and publishes the result.

## Modules

| Module | Responsibility |
|---|---|
| `vros-contracts` | Plain-Java event records and topic constants |
| `vros-domain` | Job/solution models, statuses, ports |
| `vros-application` | `OptimizationService` use cases |
| `vros-infrastructure` | JPA adapters, Kafka producer/consumers, Micrometer metrics |
| `vros-app` | REST API, security, STOMP/WebSocket, demo page |
| `optimization-worker` | Standalone Spring Boot worker: listeners, solver, retries/DLQ |

## Running

```bash
docker compose up -d db keycloak kafka kafka-ui
./mvnw -q -DskipTests install

# Terminal 1 - API (dev profile bypasses Keycloak)
./mvnw -pl vros-app spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2 - worker
./mvnw -pl optimization-worker spring-boot:run

# Submit and follow one job (requires a scenario; see http/scenarios.http)
./scripts/demo-optimization.sh 1
```

- API: <http://localhost:8080>
- Worker actuator: <http://localhost:8082/actuator/health>
- Kafka UI: <http://localhost:8090>
- Live demo page: <http://localhost:8080/optimization-demo.html>

## REST API

| Method | Path | Roles | Notes |
|---|---|---|---|
| `POST` | `/api/v1/optimization/jobs` | ADMIN | `{ "scenarioId": 1 }`, returns `202` + `Location` |
| `GET` | `/api/v1/optimization/jobs/{jobId}` | ADMIN, DRIVER | status/progress/phase |
| `GET` | `/api/v1/optimization/jobs/{jobId}/solution` | ADMIN, DRIVER | `409` until completed |
| `GET` | `/api/v1/optimization/jobs?scenarioId=&status=` | ADMIN, DRIVER | list |

Status lifecycle:

```
PENDING ─► QUEUED ─► ACCEPTED ─► RUNNING ─► COMPLETED
   └─ publish failure ─► SUBMISSION_FAILED
                     └─ REJECTED ─► RETRYING ─► ...
                     └─ FAILED / DLQ ─► FAILED
```

The **result event is the source of truth** for `COMPLETED`: a `COMPLETED` ack arriving before
the result only updates intermediate state.

## Solver

`NearestNeighborVrpSolver` is a greedy capacitated VRP:

1. For each vehicle, start at its depot.
2. Repeatedly pick the nearest remaining delivery that still fits.
3. Add the return leg to the depot.
4. Report deliveries that no vehicle can serve in `unassignedDeliveryIds` (`feasible=false`).

Distance uses the Haversine great-circle formula. Complexity is O(V × N²); a spatial index or
OR-Tools is the natural next step for larger scenarios.

## Retry and DLQ

See [dlq-runbook.md](dlq-runbook.md) for inspection and replay commands.

- Listener failures: `@RetryableTopic` (4 attempts total, exponential backoff) →
  `requests-retry-*` → `requests-dlt`. `DeserializationException` and
  `UnsupportedSchemaVersionException` bypass retries.
- Solver failures after the listener returned: in-task retry (`max-solver-attempts`) → explicit
  DLT publish + `FAILED` ack.
- Result persistence failures on the VROS side: retry → `results-dlt` → job `FAILED`.

## Exactly-once boundary

The `eos` profile enables a transactional producer on the worker, a Kafka transaction around
result + `COMPLETED` ack, and `read_committed` consumers on VROS. This gives exactly-once for the
result/ack pair and for the listener's offset + `ACCEPTED` ack.

It is **not** end-to-end exactly-once: the listener commits its offset when it hands work to the
async executor, before the solver finishes. Full end-to-end EOS would need a blocking listener or
a transactional outbox. Result handling is idempotent (unique `job_id` solution row), so duplicate
results are harmless. See [benchmark.md](benchmark.md) for the measured trade-off.

## Scaling notes

- `requests` has 6 partitions, keyed by `jobId`; add worker instances or listener concurrency to
  scale consumption.
- `ThreadPoolTaskExecutor` grows to `max-size` only after the queue fills. Set
  `vros.worker.pool.queue-capacity=0` for a strict concurrency limit.
- With multiple VROS instances, the shared `vros-api` consumer group delivers each progress event
  to only one instance. Use a per-instance group (with idempotent DB updates), a pub/sub fan-out
  (e.g. Redis), or a STOMP broker relay for WebSocket fan-out.
