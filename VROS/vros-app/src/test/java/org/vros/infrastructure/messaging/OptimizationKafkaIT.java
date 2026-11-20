package org.vros.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.OptimizationAckEvent;
import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.contracts.optimization.RoutePlan;
import org.vros.contracts.optimization.RouteStop;
import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationJobStatus;
import org.vros.domain.model.Scenario;
import org.vros.domain.port.OptimizationJobRepository;
import org.vros.domain.port.OptimizationSolutionRepository;
import org.vros.domain.port.ScenarioRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VROS-side consumer test: ACK/progress/result events update the job and store the solution.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 3,
        topics = {
                "vros.optimization.requests",
                "vros.optimization.acks",
                "vros.optimization.results",
                "vros.optimization.progress",
                "vros.optimization.requests-dlt",
                "vros.optimization.results-dlt",
                "vros.optimization.requests-retry-0",
                "vros.optimization.requests-retry-1",
                "vros.optimization.requests-retry-2",
                "vros.optimization.results-retry-0",
                "vros.optimization.results-retry-1"
        },
        brokerProperties = {
                "offsets.topic.replication.factor=1",
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1"
        })
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true",
        "spring.kafka.admin.auto-create=true",
        "vros.kafka.result-retry-backoff-ms=50",
        "vros.kafka.result-retry-max-backoff-ms=100"
})
class OptimizationKafkaIT {

    private static final String ACKS = "vros.optimization.acks";
    private static final String PROGRESS = "vros.optimization.progress";
    private static final String RESULTS = "vros.optimization.results";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private OptimizationJobRepository jobRepository;

    @Autowired
    private OptimizationSolutionRepository solutionRepository;

    @Test
    void ackProgressAndResultUpdateTheJob() {
        Scenario scenario = scenarioRepository.save(Scenario.builder().name("kafka-it-" + UUID.randomUUID()).build());
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        jobRepository.save(OptimizationJob.builder()
                .id(jobId)
                .scenarioId(scenario.getId())
                .status(OptimizationJobStatus.QUEUED)
                .progress(0)
                .phase("QUEUED")
                .attempts(0)
                .createdAt(now)
                .updatedAt(now)
                .build());

        kafkaTemplate.send(ACKS, jobId.toString(),
                new OptimizationAckEvent(1, jobId, AckStatus.ACCEPTED, "accepted", Instant.now()));

        await(() -> jobRepository.findById(jobId)
                .map(job -> job.getStatus() == OptimizationJobStatus.ACCEPTED)
                .orElse(false), Duration.ofSeconds(20), "ACK ACCEPTED");

        kafkaTemplate.send(PROGRESS, jobId.toString(),
                new OptimizationProgressEvent(1, jobId, 50, "ASSIGNING_DELIVERIES",
                        "Assigned half", Instant.now()));

        await(() -> jobRepository.findById(jobId)
                .map(job -> job.getStatus() == OptimizationJobStatus.RUNNING && job.getProgress() == 50)
                .orElse(false), Duration.ofSeconds(20), "progress 50");

        kafkaTemplate.send(RESULTS, jobId.toString(),
                new OptimizationResultEvent(
                        1, jobId, scenario.getId(), "GREEDY_NEAREST_NEIGHBOR",
                        4.448, 3, true, List.of(),
                        List.of(new RoutePlan(1L, "Van", 5, 4.448,
                                List.of(new RouteStop(1, 10L, 0, 0.01, 2, 1.111)))),
                        Instant.now()));

        await(() -> jobRepository.findById(jobId)
                .map(job -> job.getStatus() == OptimizationJobStatus.COMPLETED)
                .orElse(false), Duration.ofSeconds(20), "COMPLETED");

        OptimizationJob completed = jobRepository.findById(jobId).orElseThrow();
        assertThat(completed.getProgress()).isEqualTo(100);
        assertThat(completed.getPhase()).isEqualTo("COMPLETED");
        assertThat(completed.getCompletedAt()).isNotNull();

        var solution = solutionRepository.findByJobId(jobId).orElseThrow();
        assertThat(solution.getAlgorithm()).isEqualTo("GREEDY_NEAREST_NEIGHBOR");
        assertThat(solution.getRoutes()).hasSize(1);
        assertThat(solution.getRoutes().get(0).getStops()).hasSize(1);
    }

    private void await(BooleanSupplier condition, Duration timeout, String description) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting for " + description, ex);
            }
        }
        throw new AssertionError("Timed out waiting for " + description);
    }
}
