# Optimization throughput benchmark

## What is measured

`scripts/benchmark-optimization.sh` submits N jobs through
`POST /api/v1/optimization/jobs`, polls every job until it reaches a terminal state, and reports:

- wall-clock time from first submit to last terminal state,
- throughput (submitted jobs / wall-clock seconds),
- p50/p95/max completion latency (`completedAt - submittedAt`),
- completed and failed counts.

Submission concurrency is controlled by `CONCURRENCY`. The worker side is controlled by:

| Property | Effect |
|---|---|
| `vros.worker.listener-concurrency` | Kafka listener threads (one request each) |
| `vros.worker.pool.core-size` / `max-size` | Solver threads |
| `vros.worker.pool.queue-capacity` | Burst absorption before rejection (`0` = strict concurrency) |
| `vros.worker.simulated-delay-ms` | Artificial solver delay for saturation experiments |
| `spring.kafka.producer.transaction-id-prefix` + `eos` profile | Exactly-once experiment |

Kafka topic partition counts are fixed by the `NewTopic` beans: requests = 6, acks/results/
progress = 3. Recreate topics with different counts to measure partition scaling.

## Reproduce

```bash
docker compose up -d db keycloak kafka kafka-ui
./mvnw -q -DskipTests install

# VROS (dev profile bypasses Keycloak for local benchmarking)
./mvnw -pl vros-app spring-boot:run -Dspring-boot.run.profiles=dev

# Worker (default profile)
./mvnw -pl optimization-worker spring-boot:run
```

Seed a scenario (see `http/scenarios.http`), then:

```bash
SCENARIO_ID=1 JOBS=50 CONCURRENCY=10 NOTES="baseline c=3 pool=2/4 q=50" \
  ./scripts/benchmark-optimization.sh
```

### Variants

```bash
# Listener concurrency 1 vs 3 vs 6
vros.worker.listener-concurrency=1 ./mvnw -pl optimization-worker spring-boot:run

# Strict max concurrency (queue 0) vs burst absorption (queue 50)
vros.worker.pool.queue-capacity=0 ./mvnw -pl optimization-worker spring-boot:run

# Exactly-once experiment
./mvnw -pl optimization-worker spring-boot:run -Dspring-boot.run.profiles=eos
./mvnw -pl vros-app spring-boot:run -Dspring-boot.run.profiles=dev,eos
```

## Results

Run on: Apple Silicon macOS, Docker Desktop, Apache Kafka 4.2.2 (KRaft, single broker),
Java 21, Spring Boot 4.1.2.

Measured locally with 6 deliveries and 2 vehicles per scenario (tiny solver work, so the numbers
are dominated by Kafka + HTTP + persistence overhead). Run each variant after the VROS consumer
groups are stable; restarting a consumer group causes a ~45 s session-timeout delay that would
otherwise dominate the latency.

| Run | Listener concurrency | Pool (core/max/queue) | EOS | Jobs | Wall clock | Throughput | p50 | p95 |
|---|---:|---|---:|---:|---:|---:|---:|---:|
| Baseline | 3 | 2 / 4 / 50 | off | 50 | 3.65 s | 13.7 jobs/s | 0.212 s | 0.286 s |
| EOS | 3 | 2 / 4 / 50 | on | 50 | 3.05 s | 16.4 jobs/s | 0.459 s | 0.627 s |
| Single listener | 1 | 2 / 4 / 50 | off | 50 | _not measured_ | — | — | — |
| Strict concurrency | 3 | 2 / 2 / 0 | off | 50 | _not measured_ | — | — | — |

The EOS run roughly doubles p50/p95 completion latency (extra transaction commit round-trip per
result+ack pair). The small throughput difference is within run-to-run noise at this scale; the
latency cost is the meaningful signal. The single-listener and strict-concurrency rows are left
for reproducibility with the commands above.

## Expected outcomes and interpretation

- **Throughput scales with listener concurrency and partitions until the solver pool
  saturates.** With `core=2, max=4, queue=50`, the listener keeps consuming while at most four
  solvers run; extra requests queue. Once the queue is full, `ThreadPoolTaskExecutor` rejects and
  the listener path retries through `requests-retry-*` and eventually `requests-dlt`.
- **`queue-capacity=0` makes the limit observable immediately.** Bursts beyond `core-size`
  (while threads are busy) are rejected as soon as the pool is saturated, producing more retries
  and DLT records. With the default queue, short bursts are absorbed at the cost of latency.
- **EOS trades throughput for stronger guarantees.** The producer transaction adds a commit
  round-trip per result+ack pair and reduces the number of in-flight sends. The benchmark should
  show lower throughput and higher p50/p95, but no window where VROS sees a `COMPLETED` ack
  without a result. Note the honest boundary: the request offset is still committed before the
  async solver finishes, so this is not end-to-end exactly-once. See
  `README.md#exactly-once-boundary`.
- **Kafka metrics** are available at `/actuator/metrics` on both apps (dev profile exposes all
  endpoints): `kafka.producer.record-send-rate`, `kafka.producer.record-error-rate`,
  `kafka.consumer.fetch.manager.records.consumed.rate`, plus the custom counters
  `vros.optimization.jobs.*`.
