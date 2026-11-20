# Optimization DLQ runbook

## Topology

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `vros.optimization.requests` | VROS | `optimization-worker` | Incoming VRP jobs |
| `vros.optimization.requests-retry-0/1/2` | worker retry machinery | `optimization-worker` | Listener retries, with increasing backoff |
| `vros.optimization.requests-dlt` | worker retry machinery / async failure path | `optimization-worker-dlq` | Dead-letter queue |
| `vros.optimization.acks` | worker | `vros-api` | ACCEPTED / REJECTED / COMPLETED / FAILED |
| `vros.optimization.progress` | worker | `vros-api` | Progress updates, fanned out over STOMP |
| `vros.optimization.results` | worker | `vros-api` | Final routes |
| `vros.optimization.results-retry-0/1` | VROS retry machinery | `vros-api` | Result persistence retries |
| `vros.optimization.results-dlt` | VROS retry machinery | `vros-api-dlq` | Result dead-letter queue |

## Inspect

Kafka UI: <http://localhost:8090> → cluster `vros` → Topics.

Command line (from the repo root):

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic vros.optimization.requests-dlt \
  --from-beginning \
  --property print.key=true \
  --property print.headers=true
```

The DLT records carry the original payload plus `kafka_dlt-exception-message`,
`kafka_dlt-exception-fqcn`, `kafka_dlt-original-topic` and
`kafka_dlt-original-partition/offset` headers. The worker DLQ listener also publishes a
`FAILED` ack, so VROS marks the job `FAILED` with the reason.

## Retry semantics

- Listener-level failure (unsupported schema, poison message, saturated executor):
  `@RetryableTopic` retries three times (`attempts = 4` total) with exponential backoff,
  then publishes to `requests-dlt`. `DeserializationException` and
  `UnsupportedSchemaVersionException` are excluded and go straight to the DLT.
- Solver-level failure after the listener returned: the async task retries the solver in-process
  (`vros.worker.max-solver-attempts`), then publishes `FAILED` and the original request to
  `requests-dlt` itself. Kafka cannot redeliver an async failure because the offset was
  committed when the listener returned.
- Result persistence failure on the VROS side: the result listener retries twice, then the
  record goes to `results-dlt` and the job is marked `FAILED`.

## Replay

Replay a single payload after fixing the root cause:

```bash
# 1. Read the payload (value only) and keep the key.
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic vros.optimization.requests-dlt \
  --from-beginning --max-messages 1 \
  --property print.key=true > /tmp/dlt-record.txt

# 2. Extract key/value, then republish to the requests topic.
#    (Or use Kafka UI: message → "Produce to another topic".)
KEY=$(awk '{print $1}' /tmp/dlt-record.txt | head -1)
VALUE=$(cut -f2- /tmp/dlt-record.txt | head -1)
echo "$VALUE" | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic vros.optimization.requests \
  --property "parse.key=true" \
  --property "key.separator=:" <<< "$KEY:$VALUE"
```

Notes:

- The worker deduplicates by `jobId` for one hour, so replaying a job that already completed just
  re-publishes its last result instead of solving it again.
- After a DLT replay, VROS will receive a new `ACCEPTED`/`COMPLETED` ack. A job previously marked
  `FAILED` is terminal in the current implementation; create a new job if the state must move
  back to `RUNNING`. Production systems usually add an explicit `retry` endpoint that resets the
  status before republishing.
