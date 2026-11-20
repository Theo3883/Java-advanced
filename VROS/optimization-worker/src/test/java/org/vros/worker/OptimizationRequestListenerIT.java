package org.vros.worker;

import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.DeliverySpec;
import org.vros.contracts.optimization.OptimizationAckEvent;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.contracts.optimization.VehicleSpec;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end worker test against an embedded Kafka broker: request -> ACCEPTED ack -> result.
 */
@SpringBootTest(classes = OptimizationWorkerApplication.class)
@EmbeddedKafka(
        partitions = 3,
        topics = {
                "vros.optimization.requests",
                "vros.optimization.acks",
                "vros.optimization.results",
                "vros.optimization.progress",
                "vros.optimization.requests-dlt",
                "vros.optimization.requests-retry-0",
                "vros.optimization.requests-retry-1",
                "vros.optimization.requests-retry-2"
        },
        brokerProperties = {
                "offsets.topic.replication.factor=1",
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1"
        })
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.auto-create=true",
        "spring.kafka.listener.auto-startup=true",
        "vros.worker.pool.core-size=2",
        "vros.worker.pool.max-size=2",
        "vros.worker.pool.queue-capacity=10",
        "vros.worker.listener-concurrency=1",
        "vros.worker.simulated-delay-ms=0",
        "vros.worker.progress-throttle-ms=0"
})
class OptimizationRequestListenerIT {

    private static final String ACKS = "vros.optimization.acks";
    private static final String RESULTS = "vros.optimization.results";
    private static final String REQUESTS = "vros.optimization.requests";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    private Consumer<String, Object> consumer;

    @BeforeEach
    void subscribe() {
        consumer = consumerFactory.createConsumer("worker-it-" + UUID.randomUUID(), null);
        consumer.subscribe(List.of(ACKS, RESULTS));
    }

    @AfterEach
    void close() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void requestProducesAcceptedAckAndResult() {
        UUID jobId = UUID.randomUUID();
        OptimizationRequestEvent request = new OptimizationRequestEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION,
                jobId,
                42L,
                "it-scenario",
                List.of(new VehicleSpec(1L, "Van", 10, 0, 0)),
                List.of(new DeliverySpec(1L, 100L, 2, 0, 0.01),
                        new DeliverySpec(2L, 101L, 3, 0, 0.02)),
                Instant.now());

        kafkaTemplate.send(REQUESTS, jobId.toString(), request);

        OptimizationAckEvent ack = await(OptimizationAckEvent.class,
                event -> event.jobId().equals(jobId) && event.status() == AckStatus.ACCEPTED,
                Duration.ofSeconds(20));
        assertThat(ack.status()).isEqualTo(AckStatus.ACCEPTED);
        assertThat(ack.message()).isEqualTo("Optimization accepted");

        OptimizationResultEvent result = await(OptimizationResultEvent.class,
                event -> event.jobId().equals(jobId), Duration.ofSeconds(20));
        assertThat(result.feasible()).isTrue();
        assertThat(result.routes()).hasSize(1);
        assertThat(result.routes().get(0).stops()).hasSize(2);
        assertThat(result.unassignedDeliveryIds()).isEmpty();
    }

    @Test
    void unsupportedSchemaIsDeadLetteredAndFailsTheJob() {
        UUID jobId = UUID.randomUUID();
        OptimizationRequestEvent request = new OptimizationRequestEvent(
                999, jobId, 42L, "bad-schema", List.of(), List.of(), Instant.now());

        kafkaTemplate.send(REQUESTS, jobId.toString(), request);

        OptimizationAckEvent failed = await(OptimizationAckEvent.class,
                event -> event.jobId().equals(jobId) && event.status() == AckStatus.FAILED,
                Duration.ofSeconds(30));
        assertThat(failed.message()).contains("Dead-lettered");
    }

    private <T> List<T> awaitAll(Class<T> type, java.util.function.Predicate<T> predicate, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<T> matches = new ArrayList<>();
        while (System.nanoTime() < deadline) {
            for (var record : consumer.poll(Duration.ofMillis(250))) {
                if (type.isInstance(record.value())) {
                    T value = type.cast(record.value());
                    if (predicate.test(value)) {
                        matches.add(value);
                    }
                }
            }
            if (!matches.isEmpty()) {
                return matches;
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " matching the predicate within " + timeout);
    }

    private <T> T await(Class<T> type, java.util.function.Predicate<T> predicate, Duration timeout) {
        return awaitAll(type, predicate, timeout).get(0);
    }
}
