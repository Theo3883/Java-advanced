# VROS + OptimizationWorker — Kafka VRP Implementation Plan

> Goal: implement the **advanced** variant directly (no throwaway simple queue version):
> Kafka, a separate Spring Boot worker, a greedy VRP solver, asynchronous bounded execution,
> retry topics + DLQ, exactly-once experiments with metrics, and STOMP/WebSocket progress.

---

## 1. Target architecture

```
                       ┌──────────────────────────────────────────────┐
  REST client ────────►│                 VROS API                     │
  (submit / status /   │  OptimizationController                      │
   solution)           │  OptimizationService                         │
                       │  OptimizationJobRepository (PostgreSQL)      │
                       │  KafkaOptimizationEventPublisher             │
                       └───────┬──────────────────────────▲───────────┘
                               │                          │
             vros.optimization.requests                   │ acks
                               │                          │ results
                               ▼                          │ progress
                       ┌──────────────────────────────────────────────┐
                       │            OptimizationWorker                │
                       │  @KafkaListener (retry topics + DLT)         │
                       │  @Async bounded thread pool                  │
                       │  NearestNeighborVrpSolver                    │
                       │  Kafka event publisher                       │
                       └──────────────────────────────────────────────┘
                               │
                               ▼
                    STOMP /topic/optimization/{jobId}
```

Key decisions:

| Decision | Choice | Why |
|---|---|---|
| Broker | Apache Kafka (KRaft, single node locally) | Required; partitions, consumer groups, retry topics, transactions, metrics |
| Worker | New Maven module `optimization-worker`, separate Spring Boot app | Separate deployable, same reactor/build |
| Shared events | New plain-Java module `vros-contracts` | Worker must not depend on VROS domain/application/infrastructure |
| Job state owner | VROS API + PostgreSQL | Worker stays stateless and horizontally scalable |
| Async model | Listener acknowledges and returns; `@Async` bounded pool solves | Worker can keep consuming while jobs run |
| Retry/DLQ | `@RetryableTopic` for listener-level failures; explicit DLT publish for async solver failures | Both paths converge on one DLQ handler |
| Progress | Kafka `progress` topic → VROS → STOMP | Satisfies the WebSocket requirement |
| Exactly-once | EOS profile for request→ack; idempotent result handling + transactional result send | Honest guarantee boundaries, measurable throughput |
| Security | REST: ADMIN submits, ADMIN/DRIVER reads; STOMP: JWT on CONNECT | Fits existing Keycloak setup |

---

## 2. Current codebase constraints to respect

- **Spring Boot 4.1.2**, Spring Kafka 4.1.2, Kafka client 4.2.2, Java 17.
- Clean-architecture modules:
  - `vros-domain` — no Spring, no infrastructure/application/api.
  - `vros-application` — no infrastructure/api.
  - `vros-infrastructure` — persistence/messaging adapters.
  - `vros-app` — REST/security/websocket.
- `ArchitectureTest` enforces those rules. New code must keep them green.
- `ScenarioRepository.findByIdWithAssociations` **does not load** `vehicle.depot` or
  `delivery.customer`. The existing `EntityMapper.toDomain(ScenarioEntity)` deliberately maps
  shallow objects. **Do not reuse it for optimization.** Add a dedicated problem read port.
- Tests use H2 + Liquibase (`application-test.properties`), so the new schema must be portable
  (avoid PostgreSQL-only `JSONB`; use `TEXT` for serialized routes).
- Existing `MessageConfig` is i18n/validation, not messaging. Name the new config `KafkaConfig`.
- Security is fail-closed in `prod`, bypassed in `dev` (`vros.security.dev-bypass=true`).

---

## 3. New modules and dependency graph

```
vros-parent
├── vros-contracts          (new, plain Java + Jackson annotations)
├── vros-domain             (unchanged dependency direction)
├── vros-application        (depends on domain + contracts)
├── vros-infrastructure     (depends on domain + application + contracts + spring-kafka)
├── vros-app                (depends on all VROS modules + spring-kafka + websocket)
└── optimization-worker     (new Boot app, depends only on contracts + Kafka + actuator)
```

### 3.1 `vros-contracts/pom.xml`

- Parent: `vros-parent`.
- Dependencies: `com.fasterxml.jackson.core:jackson-annotations` (managed by Boot BOM).
- No Spring, no Lombok.

### 3.2 `optimization-worker/pom.xml`

- Parent: `vros-parent`.
- Dependencies:
  - `org.vros:vros-contracts`
  - `spring-boot-starter` (logging/config)
  - `spring-boot-starter-kafka`
  - `spring-boot-starter-json` (Jackson `JavaTimeModule`)
  - `spring-boot-starter-actuator`
  - `spring-boot-starter-validation` (optional, for request validation)
  - Lombok (optional)
  - `spring-boot-starter-test` (test)
  - `spring-boot-starter-kafka-test` (test, embedded Kafka)
- `spring-boot-maven-plugin` for the executable jar.
- `server.port=8082` (8080 VROS, 8081 Keycloak, 8090 Kafka UI).

### 3.3 Existing POM changes

- Parent `<modules>`: add `vros-contracts`, `optimization-worker`.
- Parent `<dependencyManagement>`: add `vros-contracts`.
- `vros-application`: add `vros-contracts`.
- `vros-infrastructure`: add `vros-contracts`, `spring-boot-starter-kafka`.
- `vros-app`: add `vros-contracts`, `spring-boot-starter-kafka`, `spring-boot-starter-websocket`,
  `spring-boot-starter-kafka-test` (test), `spring-boot-starter-websocket-test` (test).

### 3.4 File map (VROS side)

```
vros-contracts/src/main/java/org/vros/contracts/optimization/
  OptimizationRequestEvent.java, OptimizationAckEvent.java,
  OptimizationProgressEvent.java, OptimizationResultEvent.java,
  VehicleSpec.java, DeliverySpec.java, RoutePlan.java, RouteStop.java,
  AckStatus.java, OptimizationTopics.java

vros-domain/src/main/java/org/vros/domain/
  model/OptimizationJob.java, OptimizationJobStatus.java, OptimizationSolution.java,
        OptimizationRoute.java, OptimizationStop.java, OptimizationProblem.java,
        ProblemVehicle.java, ProblemDelivery.java
  exception/InvalidOptimizationProblemException.java,
            OptimizationJobNotReadyException.java,
            OptimizationSubmissionException.java
  port/OptimizationJobRepository.java, OptimizationSolutionRepository.java,
       OptimizationProblemRepository.java

vros-application/src/main/java/org/vros/application/
  port/OptimizationEventPublisher.java, OptimizationProgressNotifier.java
  service/OptimizationService.java
  impl/OptimizationServiceImpl.java

vros-infrastructure/src/main/java/org/vros/infrastructure/
  persistence/entity/OptimizationJobEntity.java, OptimizationSolutionEntity.java
  persistence/springdata/OptimizationJobJpaRepository.java,
                            OptimizationSolutionJpaRepository.java
  persistence/adapter/OptimizationJobRepositoryAdapter.java,
                       OptimizationSolutionRepositoryAdapter.java,
                       OptimizationProblemAdapter.java
  persistence/mapper/OptimizationMapper.java
  messaging/KafkaOptimizationEventPublisher.java, OptimizationAckListener.java,
            OptimizationResultListener.java, OptimizationProgressListener.java,
            OptimizationResultDlqListener.java
  config/KafkaConfig.java, OptimizationKafkaProperties.java
  config/ApplicationConfig.java (wire OptimizationService)

vros-app/src/main/java/org/vros/api/
  rest/v1/OptimizationController.java
  dto/request/SubmitOptimizationRequest.java
  dto/response/OptimizationJobResponse.java, OptimizationSolutionResponse.java
  mapper/OptimizationDtoMapper.java
  config/WebSocketConfig.java
  websocket/StompAuthChannelInterceptor.java, StompProgressBroadcaster.java
  exception/GlobalExceptionHandler.java (new handlers)

vros-app/src/main/resources/
  db/changelog/changes/04-create-optimization-schema.xml
  db/changelog/db.changelog-master.xml (include 04)
  static/optimization-demo.html (optional STOMP demo)

http/optimization.http
scripts/benchmark-optimization.sh
docs/optimization/benchmark.md
```

---

## 4. Event contracts (`vros-contracts`)

Package: `org.vros.contracts.optimization`.

All records carry `schemaVersion` for forward compatibility and use ISO-8601 `Instant`.
Annotate with `@JsonIgnoreProperties(ignoreUnknown = true)`.

```java
public record OptimizationRequestEvent(
        int schemaVersion,
        UUID jobId,
        Long scenarioId,
        String scenarioName,
        List<VehicleSpec> vehicles,
        List<DeliverySpec> deliveries,
        Instant requestedAt) {}

public record VehicleSpec(
        Long id, String name, int capacity, double depotLat, double depotLng) {}

public record DeliverySpec(
        Long id, Long customerId, int quantity, double lat, double lng) {}

public enum AckStatus { ACCEPTED, REJECTED, RUNNING, COMPLETED, FAILED }

public record OptimizationAckEvent(
        int schemaVersion, UUID jobId, AckStatus status, String message, Instant at) {}

public record OptimizationProgressEvent(
        int schemaVersion, UUID jobId, int progress, String phase,
        String message, Instant at) {}

public record OptimizationResultEvent(
        int schemaVersion,
        UUID jobId,
        Long scenarioId,
        String algorithm,
        double totalDistanceKm,
        long computationMillis,
        boolean feasible,
        List<Long> unassignedDeliveryIds,
        List<RoutePlan> routes,
        Instant completedAt) {}

public record RoutePlan(
        Long vehicleId, String vehicleName, int load,
        double distanceKm, List<RouteStop> stops) {}

public record RouteStop(
        int sequence, Long deliveryId, double lat, double lng,
        int quantity, double legDistanceKm) {}
```

Add `OptimizationTopics` constants used as defaults in both apps and in tests:

```java
public final class OptimizationTopics {
    public static final String REQUESTS  = "vros.optimization.requests";
    public static final String ACKS      = "vros.optimization.acks";
    public static final String RESULTS   = "vros.optimization.results";
    public static final String PROGRESS  = "vros.optimization.progress";
    public static final String REQUESTS_DLT = REQUESTS + "-dlt";
    public static final String RESULTS_DLT  = RESULTS + "-dlt";
}
```

Jackson configuration in both apps:

```java
ObjectMapper mapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
```

Use `JsonSerializer` / `JsonDeserializer` with this mapper; keep type headers enabled
(`__TypeId__`) so one consumer factory can deserialize the different event records.
Wrap the deserializer in `ErrorHandlingDeserializer` so poison messages reach the DLQ instead
of killing the listener container.

---

## 5. Kafka topology

| Topic | Partitions | Key | Producer | Consumer group | Purpose |
|---|---:|---|---|---|---|
| `vros.optimization.requests` | 6 | `jobId` | VROS | `optimization-worker` | Submit VRP jobs |
| `vros.optimization.requests-retry-0` | 6 | `jobId` | `@RetryableTopic` | `optimization-worker` | Retry attempt 1 |
| `vros.optimization.requests-retry-1` | 6 | `jobId` | `@RetryableTopic` | `optimization-worker` | Retry attempt 2 |
| `vros.optimization.requests-retry-2` | 6 | `jobId` | `@RetryableTopic` | `optimization-worker` | Retry attempt 3 |
| `vros.optimization.requests-dlt` | 3 | `jobId` | `@RetryableTopic` / worker | `optimization-worker-dlq` | Dead-letter queue |
| `vros.optimization.acks` | 3 | `jobId` | Worker | `vros-api` | ACK / REJECTED / FAILED |
| `vros.optimization.results` | 3 | `jobId` | Worker | `vros-api` | Final routes |
| `vros.optimization.progress` | 3 | `jobId` | Worker | `vros-api` | Progress events |
| `vros.optimization.results-retry-0/1` | 3 | `jobId` | VROS result listener | `vros-api` | Result retry (auto-created) |
| `vros.optimization.results-dlt` | 1 | `jobId` | VROS result listener | `vros-api-dlq` | Result-processing DLQ |

Notes:

- Keying by `jobId` preserves per-job ordering inside each topic.
- `requests` has 6 partitions so multiple worker instances / listener concurrency can scale.
- Pre-create topics with `NewTopic` beans (`KafkaAdmin`) in both apps. The `@RetryableTopic`
  retry/DLT topics can be auto-created, but pre-creating them makes partition counts deterministic.
- Set `spring.kafka.admin.fail-fast=false` locally so an app can boot while Kafka is starting.
  In production, set it to `true`.

---

## 6. VROS domain, persistence, and job lifecycle

### 6.1 Domain additions (`vros-domain`)

`org.vros.domain.model`:

- `OptimizationJob`
  - `UUID id`
  - `Long scenarioId`
  - `OptimizationJobStatus status`
  - `int progress`
  - `String phase`
  - `String errorMessage`
  - `int attempts`
  - `Instant createdAt`, `updatedAt`, `completedAt`
- `OptimizationJobStatus`
  - `PENDING`, `QUEUED`, `ACCEPTED`, `RUNNING`, `RETRYING`, `COMPLETED`, `FAILED`,
    `SUBMISSION_FAILED`
- `OptimizationSolution`
  - `UUID jobId`, `double totalDistanceKm`, `long computationMillis`,
    `String algorithm`, `boolean feasible`, `List<Long> unassignedDeliveryIds`,
    `List<OptimizationRoute> routes`, `Instant createdAt`
- `OptimizationRoute`
  - `Long vehicleId`, `String vehicleName`, `int load`, `double distanceKm`,
    `List<OptimizationStop> stops`
- `OptimizationStop`
  - `int sequence`, `Long deliveryId`, `double lat`, `double lng`,
    `int quantity`, `double legDistanceKm`
- `OptimizationProblem`
  - `Long scenarioId`, `String scenarioName`,
    `List<ProblemVehicle> vehicles`, `List<ProblemDelivery> deliveries`
- `ProblemVehicle`
  - `Long id`, `String name`, `int capacity`, `Coordinates depot`
- `ProblemDelivery`
  - `Long id`, `Long customerId`, `int quantity`, `Coordinates coordinates`

Exceptions:

- `InvalidOptimizationProblemException` → HTTP 422
- `OptimizationJobNotReadyException` → HTTP 409
- `OptimizationSubmissionException` → HTTP 503

Ports (`org.vros.domain.port`):

```java
public interface OptimizationJobRepository {
    OptimizationJob save(OptimizationJob job);
    Optional<OptimizationJob> findById(UUID id);
    List<OptimizationJob> findByScenarioId(Long scenarioId);
    List<OptimizationJob> findByStatus(OptimizationJobStatus status);
    void updateProgress(UUID id, int progress, String phase, Instant updatedAt);
    void markCompleted(UUID id, OptimizationSolution solution, Instant completedAt);
    void markFailed(UUID id, String error, OptimizationJobStatus finalStatus, Instant updatedAt);
}

public interface OptimizationSolutionRepository {
    Optional<OptimizationSolution> findByJobId(UUID jobId);
}

public interface OptimizationProblemRepository {
    Optional<OptimizationProblem> findByScenarioId(Long scenarioId);
}
```

`markCompleted` and `markFailed` are transactional adapter operations. This keeps Spring
annotations out of the application layer and makes solution + job update atomic.

### 6.2 Application layer

`org.vros.application.port` (new package):

```java
public interface OptimizationEventPublisher {
    void publishRequest(OptimizationRequestEvent event);
}

public interface OptimizationProgressNotifier {
    void notifyProgress(OptimizationProgressEvent event);
    void notifyTerminal(OptimizationJob job);
}
```

These ports use `vros-contracts`, which is plain Java. Domain stays framework-free.

`OptimizationProgressNotifier` is implemented by the API module (`StompProgressBroadcaster`).
Provide a `@ConditionalOnMissingBean` no-op implementation in infrastructure (or a test
configuration) so non-web slices and infrastructure-only tests can boot without a WebSocket
`SimpMessagingTemplate`.

`org.vros.application.service.OptimizationService`:

```java
public interface OptimizationService {
    OptimizationJob submit(Long scenarioId);
    OptimizationJob getJob(UUID jobId);
    OptimizationSolution getSolution(UUID jobId);
    List<OptimizationJob> list(Long scenarioId, OptimizationJobStatus status);
    void handleAck(OptimizationAckEvent event);
    void handleProgress(OptimizationProgressEvent event);
    void handleResult(OptimizationResultEvent event);
    void markDeadLettered(UUID jobId, String reason);
}
```

`OptimizationServiceImpl` responsibilities:

1. **Submit**
   - Load `OptimizationProblem` via `OptimizationProblemRepository`.
   - Validate: scenario exists, at least one available vehicle with depot coordinates,
     at least one delivery with coordinates, every quantity > 0.
   - Create `OptimizationJob` with `PENDING`, `progress=0`, timestamps.
   - Save it.
   - Build `OptimizationRequestEvent` (`schemaVersion=1`, `jobId`, snapshot, `Instant.now()`).
   - Publish with `OptimizationEventPublisher.publishRequest` (the adapter waits on the
     `KafkaTemplate.send(...)` future with a short timeout, e.g. 5 s).
   - On success: status `QUEUED`.
   - On failure/timeout: status `SUBMISSION_FAILED`, save, throw `OptimizationSubmissionException`.
   - Return the job.
2. **Handle ACK**
   - `ACCEPTED` → update to `ACCEPTED` if not terminal (a late ACCEPTED must not regress RUNNING).
   - `REJECTED` → `RETRYING` (not terminal; `@RetryableTopic` will retry).
   - `FAILED` → `markFailed(..., FAILED)`.
   - `COMPLETED` → do **not** mark completed; wait for the result event (see ordering note).
3. **Handle progress**
   - Ignore if job already terminal.
   - Update `progress` monotonically (never regress), `phase`, status `RUNNING`.
   - Call `OptimizationProgressNotifier.notifyProgress`.
4. **Handle result**
   - Idempotent: if a solution already exists, ignore.
   - Map contract route DTOs to domain routes.
   - `markCompleted` (job + solution in one transaction).
   - `notifyTerminal(job)`.
5. **Dead letter**
   - `markFailed(..., FAILED)` with the DLQ reason; `notifyTerminal(job)`.

Status lifecycle:

```
PENDING ──publish ok──► QUEUED ──ACK ACCEPTED──► ACCEPTED
   │                                             │
   └──publish fail──► SUBMISSION_FAILED           ├──progress──► RUNNING
                                                 │                 │
                                                 │                 ├──result──► COMPLETED
                                                 │                 └──FAILED ack / DLQ──► FAILED
                                                 └──REJECTED ack──► RETRYING ──retry──► ACCEPTED/RUNNING
```

**Ordering note:** ACK and RESULT are on different topics, so a `COMPLETED` ACK may arrive before
the result. Treat the **result event as the source of truth** for `COMPLETED`; the ACK only updates
intermediate states. This avoids a `COMPLETED` job without a retrievable solution.

### 6.3 Persistence

New Liquibase changelog: `vros-app/src/main/resources/db/changelog/changes/04-create-optimization-schema.xml`
and include it in `db.changelog-master.xml`.

```xml
<changeSet id="20-create-optimization-jobs" author="vros">
  <preConditions onFail="MARK_RAN">
    <not><tableExists tableName="optimization_jobs"/></not>
  </preConditions>
  <createTable tableName="optimization_jobs">
    <column name="id" type="UUID">
      <constraints primaryKey="true" primaryKeyName="pk_optimization_jobs"/>
    </column>
    <column name="scenario_id" type="BIGINT">
      <constraints nullable="false" foreignKeyName="fk_opt_job_scenario"
                   references="scenarios(id)" deleteCascade="true"/>
    </column>
    <column name="status" type="VARCHAR(32)">
      <constraints nullable="false"/>
    </column>
    <column name="progress" type="INT" defaultValueNumeric="0">
      <constraints nullable="false"/>
    </column>
    <column name="phase" type="VARCHAR(64)"/>
    <column name="error_message" type="TEXT"/>
    <column name="attempts" type="INT" defaultValueNumeric="0">
      <constraints nullable="false"/>
    </column>
    <column name="version" type="BIGINT" defaultValueNumeric="0">
      <constraints nullable="false"/>
    </column>
    <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
      <constraints nullable="false"/>
    </column>
    <column name="updated_at" type="TIMESTAMP WITH TIME ZONE">
      <constraints nullable="false"/>
    </column>
    <column name="completed_at" type="TIMESTAMP WITH TIME ZONE"/>
  </createTable>
  <createIndex tableName="optimization_jobs" indexName="idx_opt_jobs_scenario">
    <column name="scenario_id"/>
  </createIndex>
  <createIndex tableName="optimization_jobs" indexName="idx_opt_jobs_status">
    <column name="status"/>
  </createIndex>
</changeSet>

<changeSet id="21-create-optimization-solutions" author="vros">
  <preConditions onFail="MARK_RAN">
    <not><tableExists tableName="optimization_solutions"/></not>
  </preConditions>
  <createTable tableName="optimization_solutions">
    <column name="job_id" type="UUID">
      <constraints primaryKey="true" primaryKeyName="pk_optimization_solutions"
                   foreignKeyName="fk_opt_solution_job"
                   references="optimization_jobs(id)" deleteCascade="true"/>
    </column>
    <column name="total_distance_km" type="DOUBLE PRECISION">
      <constraints nullable="false"/>
    </column>
    <column name="computation_millis" type="BIGINT">
      <constraints nullable="false"/>
    </column>
    <column name="algorithm" type="VARCHAR(64)">
      <constraints nullable="false"/>
    </column>
    <column name="feasible" type="BOOLEAN">
      <constraints nullable="false"/>
    </column>
    <column name="unassigned_delivery_ids" type="TEXT"/>
    <column name="routes_json" type="TEXT">
      <constraints nullable="false"/>
    </column>
    <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
      <constraints nullable="false"/>
    </column>
  </createTable>
</changeSet>
```

- `routes_json` is serialized with Jackson in the adapter (portable to H2; no JSONB).
- Add `@Version long version` to `OptimizationJobEntity` because ACK, progress, and result
  listeners update the same row concurrently. Retry on `OptimisticLockingFailureException`
  (3 attempts, small backoff) or use atomic JPQL updates for progress.
- `markCompleted` must save the solution and update the job in one `@Transactional` adapter method.
- Make progress updates conditional so a late progress event cannot regress a terminal job:
  ```sql
  UPDATE optimization_jobs
     SET progress = :progress, phase = :phase, status = 'RUNNING', updated_at = :now
   WHERE id = :id
     AND status NOT IN ('COMPLETED', 'FAILED', 'SUBMISSION_FAILED')
  ```
- `markFailed` should also be conditional (`WHERE status <> 'COMPLETED'`) so a duplicate/late
  FAILED ack cannot overwrite a successful result.
- Idempotency: unique solution PK by `job_id`; duplicate results are ignored/overwritten with
  identical data.

### 6.4 Optimization problem read adapter

Do **not** use the existing `ScenarioRepository.findByIdWithAssociations` result. Add
`OptimizationProblemJpaRepository` / `OptimizationProblemAdapter`:

1. Query scenario + vehicles + depot:
   ```java
   @Query("""
       SELECT DISTINCT s FROM ScenarioEntity s
       JOIN FETCH s.vehicles v
       JOIN FETCH v.depot d
       WHERE s.id = :id
   """)
   Optional<ScenarioEntity> findScenarioWithVehiclesAndDepots(@Param("id") Long id);
   ```
2. Query deliveries + customer:
   ```java
   @Query("""
       SELECT DISTINCT d FROM ScenarioEntity s
       JOIN s.deliveries d
       JOIN FETCH d.customer c
       WHERE s.id = :id
   """)
   List<DeliveryEntity> findDeliveriesWithCustomers(@Param("id") Long id);
   ```
3. Assemble `OptimizationProblem` inside the transactional adapter.

This avoids the `MultipleBagFetchException`/Cartesian-product risk of fetching two collections
in one query and avoids the shallow mapping bug.

Validation rules:

- No scenario → `ResourceNotFoundException("Scenario", id)`.
- No available vehicle or no vehicle with depot coordinates → 422.
- No delivery or delivery without customer coordinates → 422.
- Quantity ≤ 0 → 422.
- A single delivery larger than every vehicle capacity is allowed; the solver returns
  `feasible=false` with `unassignedDeliveryIds`.

---

## 7. Worker design (`optimization-worker`)

Package root: `org.vros.worker`.

```
org.vros.worker
├── OptimizationWorkerApplication.java
├── config
│   ├── AsyncConfig.java
│   ├── KafkaConfig.java
│   └── WorkerProperties.java
├── listener
│   ├── OptimizationRequestListener.java
│   └── OptimizationDlqListener.java
├── service
│   ├── OptimizationOrchestrator.java
│   ├── OptimizationTaskService.java
│   └── WorkerEventPublisher.java
├── solver
│   ├── VrpSolver.java
│   ├── NearestNeighborVrpSolver.java
│   └── HaversineDistance.java
└── metrics
    └── WorkerMetrics.java
```

### 7.1 Request listener flow

```java
@RetryableTopic(
    attempts = "4",
    backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10_000),
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
    autoCreateTopics = "true",
    dltStrategy = DltStrategy.FAIL_ON_ERROR,
    exclude = { DeserializationException.class, UnsupportedSchemaVersionException.class })
@KafkaListener(
    topics = "${vros.kafka.topics.requests}",
    groupId = "${vros.kafka.groups.worker}",
    concurrency = "${vros.worker.listener-concurrency:3}")
public void onRequest(ConsumerRecord<String, OptimizationRequestEvent> record) {
    orchestrator.handle(record.value(), record.headers());
}
```

`OptimizationOrchestrator.handle`:

1. Validate `schemaVersion == 1`; otherwise throw `UnsupportedSchemaVersionException`
   (non-retryable → straight to DLT because of `exclude`).
2. Deduplicate by `jobId` using a bounded Caffeine/`ConcurrentHashMap` cache
   (e.g. 10 000 entries, 1 hour TTL). If already completed, re-publish the last result/ack.
3. Submit to `OptimizationTaskService.solveAsync(request)` **first**.
4. On successful submission, publish `OptimizationAckEvent(ACCEPTED)`.
5. If the executor rejects the task, publish `OptimizationAckEvent(REJECTED, "worker busy")`
   and throw `WorkerBusyException` so `@RetryableTopic` retries, then routes to DLT.
6. Return immediately. The Kafka offset is committed; the solver runs in the background.

Because the async task can start immediately, a `RUNNING` progress event may reach VROS before
the `ACCEPTED` ack. VROS state transitions must be monotonic: a late `ACCEPTED` must not regress
`RUNNING`/`COMPLETED`, and a late progress event must not regress `COMPLETED`/`FAILED`.

### 7.2 Async configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "optimizationExecutor")
    public ThreadPoolTaskExecutor optimizationExecutor(WorkerProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getPool().getCoreSize());   // default 2
        executor.setMaxPoolSize(props.getPool().getMaxSize());     // default 4
        executor.setQueueCapacity(props.getPool().getQueueCapacity()); // default 50
        executor.setThreadNamePrefix("vrp-solver-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
```

`OptimizationTaskService`:

```java
@Async("optimizationExecutor")
public CompletableFuture<Void> solveAsync(OptimizationRequestEvent request) {
    try {
        // publish RUNNING progress, run solver, publish result
        return CompletableFuture.completedFuture(null);
    } catch (Exception ex) {
        // retry in-task, then publish FAILED ack and/or DLT; never leak the exception
        return CompletableFuture.failedFuture(ex);
    }
}
```

`@Async` methods must be called through the Spring proxy (a separate bean). Exceptions from
`void` async methods go to the `AsyncUncaughtExceptionHandler`; with `CompletableFuture` the
orchestrator must attach an `exceptionally(...)` handler or the async method must handle errors
internally. Handle them internally so a solver failure can never disappear silently.

Important detail: `ThreadPoolTaskExecutor` only grows past `corePoolSize` after the queue is full.
For a strict "max concurrent solvers" limit, set `queueCapacity=0` (uses `SynchronousQueue`).
The default plan keeps a small queue to absorb bursts; document this behavior and expose both
settings in `application.properties`.

Suggested worker properties:

```properties
vros.worker.pool.core-size=2
vros.worker.pool.max-size=4
vros.worker.pool.queue-capacity=50
vros.worker.listener-concurrency=3
vros.worker.simulated-delay-ms=0
vros.worker.max-solver-attempts=3
vros.worker.retry-backoff-ms=250
vros.worker.progress-throttle-ms=250
```

### 7.3 Greedy nearest-neighbor VRP solver

`NearestNeighborVrpSolver implements VrpSolver`:

Input: `OptimizationRequestEvent` (vehicles with depot coordinates, deliveries with coordinates
and quantity). Output: `OptimizationResultEvent`.

Algorithm:

```
remaining = all deliveries
routes = []
for each vehicle in vehicles (only available, capacity > 0, depot != null):
    current = vehicle.depot
    routeStops = []
    load = 0
    routeDistance = 0
    while remaining is not empty:
        candidate = nearest delivery in remaining such that load + qty <= capacity
        if no candidate: break
        leg = haversine(current, candidate)
        routeStops.add(stop(sequence, delivery, leg))
        routeDistance += leg
        load += candidate.qty
        current = candidate
        remaining.remove(candidate)
        publishProgress(assigned / total)
    if routeStops not empty:
        routeDistance += haversine(current, vehicle.depot)   // return leg
        routes.add(route(vehicle, load, routeDistance, routeStops))
```

- Distance: Haversine in kilometres.
- If `remaining` is not empty after all vehicles: `feasible=false`,
  `unassignedDeliveryIds = remaining.ids`.
- `totalDistanceKm` = sum of route distances.
- `computationMillis` = wall-clock solver duration.
- `algorithm = "GREEDY_NEAREST_NEIGHBOR"`.
- Progress: `assigned / total * 90` while solving; `95` finalizing; `100` on completion.
  Throttle progress publishes (e.g. only when the percentage changes by ≥ 5 or every 250 ms).
- Complexity: O(V × N²) in the naive form. Acceptable for homework-sized scenarios; mention
  a spatial index or OR-Tools as a future optimization.
- Handle null/empty gracefully, but validation should have rejected bad input earlier.

### 7.4 Result publishing

`WorkerEventPublisher`:

- `publishAck(...)` → `acks` topic, key `jobId`.
- `publishProgress(...)` → `progress` topic, key `jobId`.
- `publishResult(...)` → `results` topic, key `jobId`.
- `publishToDlt(...)` → `requests-dlt` topic, key `jobId`, with original payload and error headers.
- Set `schemaVersion=1` on every event.
- Use `KafkaTemplate<String, Object>` with `acks=all` and `enable.idempotence=true`.
- In the `eos` profile, use `KafkaTemplate.executeInTransaction(...)` for result+ack pairs and
  `spring.kafka.producer.transaction-id-prefix`.

### 7.5 Async failure handling and retry

Two distinct failure paths:

| Failure | Handling |
|---|---|
| Listener-level: bad schema, deserialization, executor rejection | `@RetryableTopic` retries 3 times, then DLT |
| Solver-level: exception inside the async task | In-task `RetryTemplate` (3 attempts, exponential backoff); if still failing, publish to `requests-dlt` + `FAILED` ack |

This is deliberate: the listener returns immediately, so Kafka cannot retry an async exception
after the offset is committed. The in-task retry + explicit DLT publish gives the same observable
behavior and keeps the worker non-blocking.

Optional reliability hardening: a scheduled `OptimizationJobReaper` in VROS re-publishes jobs
stuck in `ACCEPTED`/`RUNNING` for more than N minutes (e.g. worker crashed). Add this only if time
permits; it is not required for the homework.

---

## 8. Retry and DLQ

### 8.1 Worker DLQ listener

```java
@KafkaListener(
    topics = "${vros.kafka.topics.requests-dlt}",
    groupId = "${vros.kafka.groups.worker-dlq}")
public void onDeadLetter(ConsumerRecord<String, OptimizationRequestEvent> record) {
    // log topic/partition/offset/key + kafka_dlt-* headers
    // publish FAILED ack so VROS marks the job FAILED
    // increment vros.optimization.dead_lettered counter
}
```

- Do not annotate this listener with `@RetryableTopic` (avoid infinite DLT loops).
- If the payload itself is unreadable, use `ConsumerRecord<String, byte[]>` or
  `ErrorHandlingDeserializer` and extract the `jobId` from the record key/headers.
- The DLT is the DLQ; Kafka UI makes it inspectable. No extra DB table is required.

### 8.2 VROS result DLQ listener

`OptimizationResultDlqListener` consumes `vros.optimization.results-dlt`, logs the failure, and
calls `OptimizationService.markDeadLettered(jobId, reason)`. This covers DB/mapping failures on
the VROS side.

### 8.3 Retry test

Deterministic tests:

1. **Unsupported schema version** — send a request with `schemaVersion=999`.
   `@RetryableTopic` should retry 3 times (attempts=4 total) and route to `requests-dlt`;
   the DLT listener publishes `FAILED`; assert VROS job status is `FAILED`.
2. **Executor saturation** — configure `core=1, max=1, queue=0`, make job A slow
   (`simulated-delay-ms=5000`), submit job B. B is rejected, retried, then DLQ; assert the
   retry topics receive messages and the DLT receives one.
3. **Transient solver failure** — mock `VrpSolver` to throw twice then succeed; assert the
   result is published after retry. A separate test throws always and asserts DLT + FAILED.
4. **Deserialization poison message** — publish invalid JSON to `requests`; assert the
   `ErrorHandlingDeserializer` + DLT path handles it and the container stays alive.

Use `@EmbeddedKafka` and `KafkaTestUtils` to consume retry/DLT topics and count records.
Override backoff to 50–100 ms in `application-test.properties` to keep tests fast.

---

## 9. REST API (VROS)

Controller: `org.vros.api.rest.v1.OptimizationController`
Base path: `/api/v1/optimization`.

| Method | Path | Body | Response | Security |
|---|---|---|---|---|
| `POST` | `/jobs` | `{ "scenarioId": 1 }` | `202 Accepted`, `Location`, job DTO | ADMIN |
| `GET` | `/jobs/{jobId}` | — | job status DTO | ADMIN, DRIVER |
| `GET` | `/jobs/{jobId}/solution` | — | solution DTO; 409 if not completed | ADMIN, DRIVER |
| `GET` | `/jobs?scenarioId=&status=` | — | list of job DTOs | ADMIN, DRIVER |
| `POST` | `/jobs/{jobId}/retry` (optional) | — | `202 Accepted` | ADMIN |

### 9.1 Submit response

```http
HTTP/1.1 202 Accepted
Location: /api/v1/optimization/jobs/6f1c...
Content-Type: application/json

{
  "jobId": "6f1c2b7e-...",
  "scenarioId": 1,
  "status": "QUEUED",
  "progress": 0,
  "phase": "SUBMITTED",
  "submittedAt": "2026-11-20T10:15:30Z",
  "updatedAt": "2026-11-20T10:15:30Z"
}
```

### 9.2 Status response

```json
{
  "jobId": "6f1c2b7e-...",
  "scenarioId": 1,
  "status": "RUNNING",
  "progress": 45,
  "phase": "ASSIGNING_DELIVERIES",
  "attempts": 0,
  "error": null,
  "submittedAt": "...",
  "updatedAt": "...",
  "completedAt": null
}
```

### 9.3 Solution response

```json
{
  "jobId": "6f1c2b7e-...",
  "scenarioId": 1,
  "algorithm": "GREEDY_NEAREST_NEIGHBOR",
  "totalDistanceKm": 42.73,
  "computationMillis": 12,
  "feasible": true,
  "unassignedDeliveryIds": [],
  "routes": [
    {
      "vehicleId": 1,
      "vehicleName": "Van 1",
      "load": 8,
      "distanceKm": 21.31,
      "stops": [
        {
          "sequence": 1,
          "deliveryId": 10,
          "lat": 44.4268,
          "lng": 26.1025,
          "quantity": 2,
          "legDistanceKm": 3.24
        }
      ]
    }
  ],
  "completedAt": "..."
}
```

Error mapping in `GlobalExceptionHandler`:

- `ResourceNotFoundException` → 404 (existing).
- `InvalidOptimizationProblemException` → 422 `https://vros.example/errors/invalid-optimization-problem`.
- `OptimizationJobNotReadyException` → 409 `https://vros.example/errors/job-not-ready`.
- `OptimizationSubmissionException` → 503 `https://vros.example/errors/optimization-unavailable`.

Add i18n keys to `messages.properties` and `messages_ro.properties` for the new errors.

DTOs:

- `SubmitOptimizationRequest` with `@NotNull Long scenarioId`.
- `OptimizationJobResponse`
- `OptimizationSolutionResponse` (nested route/stop DTOs)
- `OptimizationDtoMapper` (pure functions, like `DtoMapper`).

---

## 10. WebSocket / STOMP

### 10.1 Configuration (`vros-app`)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*", "https://*.vros.org");
        // Optional browser fallback: registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
```

### 10.2 Destinations

| Destination | Payload |
|---|---|
| `/topic/optimization/{jobId}` | progress/status update for one job |
| `/topic/optimization` | optional global feed |

Payload:

```json
{
  "jobId": "6f1c2b7e-...",
  "status": "RUNNING",
  "progress": 45,
  "phase": "ASSIGNING_DELIVERIES",
  "message": "Assigned 9 of 20 deliveries",
  "at": "2026-11-20T10:15:35Z"
}
```

`StompProgressBroadcaster implements OptimizationProgressNotifier`:

```java
messagingTemplate.convertAndSend("/topic/optimization/" + event.jobId(), event);
```

Also broadcast on terminal ACK/result so clients see `COMPLETED`/`FAILED` even if the last
progress event was below 100.

### 10.3 STOMP authentication

`StompAuthChannelInterceptor`:

- On `CONNECT`: read the `Authorization` native header, validate the Bearer token with the
  existing `JwtDecoder`, build a `JwtAuthenticationToken`, and set it on the STOMP headers.
- In the `dev` profile (`vros.security.dev-bypass=true`), inject a synthetic ADMIN principal
  when no token is present, mirroring `DevAuthenticationFilter`.
- On `SUBSCRIBE`: require an authenticated user with `ADMIN` or `DRIVER` for
  `/topic/optimization/**`; otherwise throw `MessagingException` / `AccessDeniedException`.
- The HTTP handshake `/ws/**` is `permitAll`; message-level authorization happens in the
  interceptor. This is the standard split for STOMP.

### 10.4 Scaling note

With a single VROS instance, the `vros-api` consumer group receives each progress event once and
broadcasts to all local WebSocket clients. With multiple VROS instances, a shared consumer group
means only one instance sees each event, so clients connected to other instances miss updates.
Options:

- Use a unique consumer group per instance for the `progress` topic (broadcast semantics), with
  idempotent DB updates; or
- Add Redis/another pub-sub fan-out; or
- Use a dedicated STOMP broker relay (RabbitMQ/ActiveMQ) instead of the simple broker.

Document the single-instance assumption and the scaling options.

---

## 11. Kafka configuration

### 11.1 Common properties

VROS `application.properties`:

```properties
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.acks=all
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=org.vros.contracts.optimization
spring.kafka.consumer.properties.spring.json.use.type.headers=true
spring.kafka.listener.ack-mode=record
spring.kafka.listener.missing-topics-fatal=false
spring.kafka.admin.fail-fast=false

vros.kafka.topics.requests=${VROS_KAFKA_REQUESTS:vros.optimization.requests}
vros.kafka.topics.acks=${VROS_KAFKA_ACKS:vros.optimization.acks}
vros.kafka.topics.results=${VROS_KAFKA_RESULTS:vros.optimization.results}
vros.kafka.topics.progress=${VROS_KAFKA_PROGRESS:vros.optimization.progress}
vros.kafka.topics.requests-dlt=${VROS_KAFKA_REQUESTS_DLT:vros.optimization.requests-dlt}
vros.kafka.topics.results-dlt=${VROS_KAFKA_RESULTS_DLT:vros.optimization.results-dlt}
vros.kafka.groups.api=vros-api
vros.kafka.groups.api-dlq=vros-api-dlq
```

Worker `application.properties` mirrors the producer/consumer settings plus:

```properties
server.port=8082
spring.application.name=OptimizationWorker
vros.kafka.groups.worker=optimization-worker
vros.kafka.groups.worker-dlq=optimization-worker-dlq
```

### 11.2 Producer/consumer factories

Prefer explicit `@Bean` factories so the custom `ObjectMapper` (JavaTimeModule, ISO instants) is
used consistently. (Adapt the `KafkaProperties.build*Properties(...)` call to the exact Boot 4
signature available in the project; the rest of the configuration is unchanged.)

```java
@Bean
ProducerFactory<String, Object> producerFactory(KafkaProperties props, ObjectMapper mapper) {
    Map<String, Object> config = props.buildProducerProperties();
    JsonSerializer<Object> serializer = new JsonSerializer<>(mapper);
    return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), serializer);
}

@Bean
ConsumerFactory<String, Object> consumerFactory(KafkaProperties props, ObjectMapper mapper) {
    Map<String, Object> config = props.buildConsumerProperties();
    JsonDeserializer<Object> json = new JsonDeserializer<>(mapper);
    json.addTrustedPackages("org.vros.contracts.optimization");
    ErrorHandlingDeserializer<Object> value = new ErrorHandlingDeserializer<>(json);
    return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), value);
}

@Bean
KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
    return new KafkaTemplate<>(pf);
}

@Bean
ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        ConsumerFactory<String, Object> cf, KafkaTemplate<String, Object> template) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
    factory.setConsumerFactory(cf);
    factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(template),
            new FixedBackOff(1000L, 2)));
    return factory;
}
```

- `@EnableKafka` on a `KafkaConfig` class in both apps to ensure retry-topic support is active.
- In VROS there is also a JPA transaction manager. If a listener needs Kafka transactions, qualify
  it explicitly (`@Transactional(transactionManager = "kafkaTransactionManager")`); otherwise use
  the JPA transaction manager and manual/record acknowledgment. Do not let Spring pick an
  arbitrary transaction manager.
- Add `NewTopic` beans for deterministic partition counts. Example:
  ```java
  @Bean NewTopic requestsTopic() {
      return TopicBuilder.name("vros.optimization.requests").partitions(6).replicas(1).build();
  }
  ```
- The `DefaultErrorHandler` above is the baseline for deserialization/poison messages.
  `@RetryableTopic` installs its own handler for annotated listeners; keep both concerns separate
  and verify with the poison-message test.

### 11.3 Exactly-once profile (`eos`)

`application-eos.properties` (worker):

```properties
spring.kafka.producer.transaction-id-prefix=vrp-worker-tx-
spring.kafka.producer.acks=all
spring.kafka.producer.properties.enable.idempotence=true
spring.kafka.consumer.properties.isolation.level=read_committed
spring.kafka.listener.ack-mode=record
```

- Annotate the request listener with `@Transactional` so the offset commit + `ACCEPTED` ack are
  atomic (worker has no JPA transaction manager, so there is no ambiguity).
- Send result + terminal ack inside `KafkaTemplate.executeInTransaction(...)`.
- VROS consumer sets `isolation.level=read_committed` and `enable.auto.commit=false`.
- **Guarantee boundary:** the async handoff means the request offset is committed before the
  solver finishes. End-to-end exactly-once would require keeping the offset uncommitted until the
  result is produced (blocking listener) or a transactional outbox/state store. Implement the
  EOS profile for the request→ack path and idempotent result handling for the result path, and
  state this explicitly in the report. This is the honest, measurable experiment.
- Optional comparison: a `sync-eos` profile where the listener runs the solver synchronously and
  publishes the result in the same Kafka transaction. Benchmark both to quantify the
  throughput/guarantee trade-off.

---

## 12. Observability and throughput measurement

### 12.1 Metrics

Custom Micrometer metrics (both apps where relevant):

- `vros.optimization.jobs.submitted` (counter, tag `scenarioId`)
- `vros.optimization.jobs.completed` (counter)
- `vros.optimization.jobs.failed` (counter, tag `reason`)
- `vros.optimization.jobs.retried` (counter)
- `vros.optimization.jobs.dead_lettered` (counter)
- `vros.optimization.solve.duration` (timer)
- `vros.optimization.inflight` (gauge, worker)
- `vros.optimization.progress.events` (counter)

Kafka client metrics are auto-bound by Boot's `KafkaMetricsAutoConfiguration`:

- Producer: `kafka.producer.record-send-rate`, `kafka.producer.record-error-rate`,
  `kafka.producer.request-latency-avg`.
- Consumer: `kafka.consumer.fetch.manager.records.lag`,
  `kafka.consumer.fetch.manager.records.consumed.rate`,
  `kafka.consumer.commit.rate`.
- Inspect via `/actuator/metrics/{name}` or a Prometheus registry.

Add `micrometer-registry-prometheus` only if a Prometheus scrape is desired; otherwise actuator
metrics are enough. In `application-dev.properties` expose the endpoints:

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.metrics.enable.kafka=true
```

(Production keeps the existing restricted actuator exposure.)

### 12.2 Benchmark script

`scripts/benchmark-optimization.sh`:

1. Submit N jobs (e.g. 200) with `xargs -P 10` or `hey`/`k6`.
2. Poll `GET /jobs/{id}` until all reach `COMPLETED`/`FAILED`.
3. Record wall-clock time, jobs/sec, p50/p95 solve duration, retry count, DLQ count.
4. Run with:
   - `worker.listener-concurrency=1` vs `3` vs `6`
   - `requests` partitions 1 vs 6 (recreate topics)
   - `eos` profile on vs off
5. Store results in `docs/optimization/benchmark.md` with the Kafka version, hardware,
   scenario size, and commit hash.

Expected outcomes to report:

- Throughput scales with partitions/consumer concurrency until the solver pool saturates.
- EOS adds latency and lowers throughput, but eliminates duplicate result/ack writes.
- With the async pool, the worker keeps consuming while solvers run; queue depth and
  `RejectedExecutionException` counts show the configured limit.

---

## 13. Docker Compose

Add to `docker-compose.yml`:

```yaml
  kafka:
    image: apache/kafka:4.2.2
    container_name: vros-kafka
    restart: unless-stopped
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,INTERNAL://:29092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,INTERNAL://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,INTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_NUM_PARTITIONS: 6
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list >/dev/null 2>&1"]
      interval: 5s
      timeout: 10s
      retries: 20

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: vros-kafka-ui
    restart: unless-stopped
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: vros
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
    depends_on:
      kafka:
        condition: service_healthy
```

- Use `localhost:9092` from host-run Spring apps; use `kafka:29092` from containers.
- `CLUSTER_ID` must be a valid KRaft cluster ID. The value above is a known example; generate a
  new one with `kafka-storage.sh random-uuid` if desired.
- Topics are created by `NewTopic` beans at app startup. If you prefer a one-shot init container,
  add a `kafka-init` service using `/opt/kafka/bin/kafka-topics.sh --create --if-not-exists`.
- Add `KAFKA_BOOTSTRAP_SERVERS=localhost:9092` to `.env.local.example` and `.env.local`.

---

## 14. Security changes

`SecurityConfig` (prod chain) additions:

```java
// WebSocket handshake: authentication happens on STOMP CONNECT
.requestMatchers("/ws", "/ws/**").permitAll()

// Optimization
.requestMatchers(HttpMethod.POST, "/api/v1/optimization/jobs").hasRole("ADMIN")
.requestMatchers(HttpMethod.POST, "/api/v1/optimization/jobs/*/retry").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET, "/api/v1/optimization/**").hasAnyRole("ADMIN", "DRIVER")
```

- Dev chain already permits everything; the STOMP interceptor injects a dev principal.
- Add `/api/v1/optimization/**` to the OpenAPI tag list.
- CORS already allows `http://localhost:*`; WebSocket endpoint uses the same origin patterns.
- Ownership: optimization jobs are scenario-level, so ADMIN/DRIVER access is sufficient for the
  homework. Per-user job ownership can be added later by linking `jobId` to the submitting user.

---

## 15. Testing strategy

| Test class | Module | What it proves |
|---|---|---|
| `NearestNeighborVrpSolverTest` | worker | Nearest-neighbor order, capacity limits, return-to-depot leg, unassigned deliveries, empty input |
| `HaversineDistanceTest` | worker | Distance sanity (known coordinate pairs) |
| `OptimizationOrchestratorTest` | worker | Dedup, ACK order, rejection when pool is full, progress throttling |
| `OptimizationRequestListenerIT` | worker | `@EmbeddedKafka`: request → ACK → result |
| `RetryAndDlqIT` | worker | Unsupported schema + saturation → retry topics → DLT → FAILED |
| `OptimizationServiceTest` | app | Submit validation, status transitions, idempotent result handling |
| `OptimizationProblemAdapterIT` | infra | Nested depot/customer coordinates loaded correctly |
| `OptimizationControllerIT` | app | 202 submit, status, solution 200/409/404, security roles |
| `OptimizationKafkaIT` | app | `@EmbeddedKafka`: ACK/progress/result consumed; DB updated |
| `OptimizationWebSocketIT` | app | STOMP connect/subscribe receives progress broadcast |
| `OptimizationContractTest` | contracts | JSON round-trip and unknown-field tolerance |
| `ArchitectureTest` | app | Existing rules still pass with the new packages |

Test profile properties (worker):

```properties
spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}
vros.worker.pool.core-size=1
vros.worker.pool.max-size=1
vros.worker.pool.queue-capacity=0
vros.worker.retry-backoff-ms=50
vros.worker.simulated-delay-ms=0
```

Test profile properties (VROS): add `spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}`
and disable the real `KafkaAdmin` fail-fast. Keep H2 + Liquibase; the new changelog uses portable
types.

**Important:** existing `@SpringBootTest` security tests (e.g. `OwnershipSecurityTest`) boot the
full context and would otherwise start Kafka listeners and try to create topics without a broker.
In `application-test.properties` set:

```properties
spring.kafka.listener.auto-startup=false
spring.kafka.admin.fail-fast=false
```

Then enable listeners only in the Kafka integration tests with
`@TestPropertySource(properties = "spring.kafka.listener.auto-startup=true")` (plus
`@EmbeddedKafka`). This keeps the existing test suite fast and isolated.

Add an `integration` Maven profile / JUnit tag for tests that start embedded Kafka, so the fast
unit suite stays quick.

---

## 16. Implementation phases

### Phase 0 — Infrastructure and contracts (0.5 day)

- Add `vros-contracts` module + event records + `OptimizationTopics`.
- Add `optimization-worker` module skeleton (main class, actuator, Kafka starter).
- Update parent POM modules/dependency management.
- Add Kafka + Kafka UI to `docker-compose.yml`; add env vars.
- Add `NewTopic` beans / topic creation.
- **Exit:** `docker compose up -d kafka kafka-ui` is healthy; both apps compile; contracts tests pass.

### Phase 1 — VROS job persistence and REST submission (1 day)

- Add domain models, status enum, exceptions, ports.
- Add `OptimizationProblemAdapter` with nested fetch queries.
- Add Liquibase `04-create-optimization-schema.xml`.
- Add JPA entities/repositories/adapters/mappers.
- Add `OptimizationService` + `OptimizationServiceImpl` (submit/status/solution/list).
- Add `OptimizationEventPublisher` port + `KafkaOptimizationEventPublisher`.
- Add REST controller, DTOs, mapper, exception handlers, security rules.
- **Exit:** `POST /jobs` persists a job and publishes to `requests`; status/solution endpoints
  return correct states; ArchUnit green.

### Phase 2 — Worker solver and async execution (1 day)

- Implement `HaversineDistance`, `VrpSolver`, `NearestNeighborVrpSolver`.
- Implement `AsyncConfig`, `OptimizationTaskService`, `OptimizationOrchestrator`.
- Implement `OptimizationRequestListener` with ACK + async submit.
- Implement `WorkerEventPublisher` (acks/results/progress).
- Add worker properties and metrics.
- **Exit:** a request produces ACCEPTED + progress + result; multiple jobs run concurrently up to
  the pool limit; unit tests pass.

### Phase 3 — VROS consumers and WebSocket (1 day)

- Add `OptimizationAckListener`, `OptimizationResultListener`, `OptimizationProgressListener`.
- Wire `OptimizationService.handleAck/handleProgress/handleResult`.
- Add `WebSocketConfig`, `StompAuthChannelInterceptor`, `StompProgressBroadcaster`.
- Add `/ws` security rules and CORS.
- Add a static `optimization-demo.html` page (optional but useful for the demo).
- **Exit:** status transitions PENDING→QUEUED→ACCEPTED→RUNNING→COMPLETED; solution retrievable;
  browser receives live progress over STOMP.

### Phase 4 — Retry, DLQ, and failure tests (0.5–1 day)

- Add `@RetryableTopic` configuration + `OptimizationDlqListener`.
- Add async solver retry + explicit DLT publish.
- Add `OptimizationResultDlqListener` on the VROS side.
- Add retry/DLQ integration tests (schema version, saturation, poison message).
- Add DLQ inspection instructions (Kafka UI) and a replay procedure.
- **Exit:** failed messages retry the configured number of times, land in the DLT, and the job
  ends as FAILED; tests prove it.

### Phase 5 — EOS, metrics, benchmark, docs (0.5–1 day)

- Add `application-eos.properties` and transactional producer/listener configuration.
- Add Micrometer counters/timers and Kafka metrics exposure.
- Add `scripts/benchmark-optimization.sh` and `docs/optimization/benchmark.md`.
- Run the benchmark with/without EOS and with different partition/concurrency settings.
- Add `http/optimization.http` and a README/demo script.
- **Exit:** measured throughput numbers documented; EOS trade-offs explained; demo reproducible.

---

## 17. Acceptance criteria mapping

| Requirement | Implementation | Proof |
|---|---|---|
| Install Kafka | `docker-compose.yml` KRaft broker + Kafka UI | `docker compose ps`, Kafka UI |
| Separate OptimizationWorker project | `optimization-worker` module, own main class/jar/port | `./mvnw -pl optimization-worker spring-boot:run` |
| VROS publishes requests | `KafkaOptimizationEventPublisher` → `requests` topic | Kafka UI / `KafkaTemplate` logs |
| Worker consumes and publishes ACKs | `OptimizationRequestListener` → `acks` | ACK listener updates job to ACCEPTED |
| Greedy nearest-neighbor VRP | `NearestNeighborVrpSolver` | Solver unit tests + result routes |
| Worker publishes solutions, VROS consumes | `results` topic + `OptimizationResultListener` | Solution endpoint returns routes |
| REST: submit | `POST /api/v1/optimization/jobs` | 202 + Location |
| REST: status | `GET /api/v1/optimization/jobs/{jobId}` | Status/progress JSON |
| REST: solution | `GET /api/v1/optimization/jobs/{jobId}/solution` | 200 / 409 / 404 |
| DLQ handler + retry test | `@RetryableTopic` + `OptimizationDlqListener` + tests | `RetryAndDlqIT` |
| Async background execution | `@Async` + bounded `ThreadPoolTaskExecutor` | Concurrency/saturation test |
| Worker accepts more messages while busy | Listener returns after submit; pool queue/limit | Submit 3 jobs, observe queue/rejection |
| Kafka scalability | 6 partitions, consumer groups, concurrency | Benchmark with 1 vs 6 partitions |
| Exactly-once experiment + metrics | `eos` profile, transactional producer, Micrometer/Kafka metrics | `benchmark.md` |
| WebSocket STOMP progress | `/ws` + `/topic/optimization/{jobId}` | `OptimizationWebSocketIT` + demo page |

---

## 18. Demo script

```bash
# 1. Infra
docker compose up -d db keycloak kafka kafka-ui

# 2. Build
./mvnw -q -DskipTests install

# 3. Run VROS (dev profile bypasses Keycloak) and the worker
./mvnw -pl vros-app spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw -pl optimization-worker spring-boot:run

# 4. Create/seed a scenario (see http/scenarios.http), then submit a job
curl -X POST http://localhost:8080/api/v1/optimization/jobs \
  -H 'Content-Type: application/json' \
  -d '{"scenarioId":1}'

# 5. Poll status
curl http://localhost:8080/api/v1/optimization/jobs/{jobId}

# 6. Fetch the solution when COMPLETED
curl http://localhost:8080/api/v1/optimization/jobs/{jobId}/solution

# 7. Live progress: open the demo page or connect to ws://localhost:8080/ws
#    SUBSCRIBE /topic/optimization/{jobId}

# 8. DLQ demo: send an unsupported schema version or saturate the pool,
#    then inspect vros.optimization.requests-dlt in Kafka UI (http://localhost:8090).
```

---

## 19. Risks, trade-offs, and decisions to document

1. **Async vs Kafka retry semantics.** The listener commits the offset after submitting the task.
   Kafka cannot retry an async solver exception. Mitigation: in-task retry + explicit DLT publish;
   document the boundary. Optional `sync-eos` profile for true end-to-end EOS comparison.
2. **Exactly-once is not end-to-end by default.** EOS covers request→ack; results are idempotent.
   Do not claim more than that in the report. A transactional outbox or blocking listener is
   required for full EOS.
3. **Optimistic locking.** ACK/progress/result listeners update the same job row from different
   topics. Add `@Version` and retry `OptimisticLockingFailureException`, or use atomic updates.
4. **Multiple collections fetch.** Avoid `JOIN FETCH` on two `@ManyToMany` sets; use two queries.
5. **Thread pool semantics.** `maxPoolSize` only applies after the queue fills. For a strict limit,
   set `queueCapacity=0`; otherwise document the burst-absorption behavior.
6. **WebSocket fan-out with multiple VROS instances.** A shared consumer group delivers each
   progress event to one instance. Use per-instance groups or a broker relay for fan-out.
7. **Kafka startup ordering.** `spring.kafka.admin.fail-fast=false` locally; `true` in production.
   The API returns 503 on publish failure and marks `SUBMISSION_FAILED`.
8. **Test flakiness.** Embedded Kafka tests need generous timeouts and short retry backoffs.
   Tag them as integration tests so the unit suite remains fast.
9. **Boot 4 API changes.** Verify `spring.kafka.*` properties and Spring Kafka 4.1 annotation
   attributes against the local dependency versions; the plan uses the documented 4.1 names.
10. **No JDK in the current shell.** Build/run from IntelliJ or set `JAVA_HOME` to a JDK 17+
    installation before running `./mvnw`.

---

## 20. Out of scope / future work

- OR-Tools / time-window / pickup-delivery VRP variants.
- Real road-network distances (OSRM/GraphHopper) instead of Haversine.
- Transactional outbox for guaranteed publish-after-commit.
- Schema Registry / Avro for event evolution.
- Persistent worker job store and crash recovery.
- Kubernetes manifests / Helm chart.
- Per-user job ownership and fine-grained WebSocket authorization.
