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
import org.vros.contracts.optimization.OptimizationAckEvent;
import org.vros.contracts.optimization.OptimizationRequestEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the retry path: a saturated worker rejects the listener call, {@code @RetryableTopic}
 * retries three times and the request lands in the DLT, where the DLQ listener emits a FAILED ack.
 */
@SpringBootTest(classes = OptimizationWorkerApplication.class)
@EmbeddedKafka(
        // Topics are created by the application's NewTopic beans so partition counts stay
        // consistent with the worker configuration.
        brokerProperties = {
                "offsets.topic.replication.factor=1",
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1"
        })
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.auto-create=true",
        "spring.kafka.listener.auto-startup=true",
        // Strict concurrency: the second request is rejected while the first solver is running.
        "vros.worker.pool.core-size=1",
        "vros.worker.pool.max-size=1",
        "vros.worker.pool.queue-capacity=0",
        "vros.worker.listener-concurrency=1",
        "vros.worker.simulated-delay-ms=4000",
        "vros.worker.listener-retry-backoff-ms=50",
        "vros.worker.listener-retry-max-backoff-ms=100",
        "vros.worker.progress-throttle-ms=0"
})
class RetryAndDlqIT {

    private static final String REQUESTS = "vros.optimization.requests";
    private static final String ACKS = "vros.optimization.acks";
    private static final String RETRY_0 = "vros.optimization.requests-retry-0";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    private Consumer<String, Object> ackConsumer;
    private Consumer<String, Object> retryConsumer;

    @BeforeEach
    void subscribe() {
        ackConsumer = consumerFactory.createConsumer("retry-it-acks-" + UUID.randomUUID(), null);
        ackConsumer.subscribe(List.of(ACKS));
        retryConsumer = consumerFactory.createConsumer("retry-it-retry-" + UUID.randomUUID(), null);
        retryConsumer.subscribe(List.of(RETRY_0));
    }

    @AfterEach
    void close() {
        if (ackConsumer != null) {
            ackConsumer.close();
        }
        if (retryConsumer != null) {
            retryConsumer.close();
        }
    }

    @Test
    void saturatedWorkerRetriesThenDeadLetters() {
        UUID slowJobId = UUID.randomUUID();
        UUID rejectedJobId = UUID.randomUUID();

        kafkaTemplate.send(REQUESTS, slowJobId.toString(), request(slowJobId));
        // Give the first request time to reach the solver, so the second submission is rejected.
        awaitAck(AckStatus.ACCEPTED, slowJobId, Duration.ofSeconds(20));

        kafkaTemplate.send(REQUESTS, rejectedJobId.toString(), request(rejectedJobId));

        OptimizationAckEvent failed = awaitAck(AckStatus.FAILED, rejectedJobId, Duration.ofSeconds(30));
        assertThat(failed.message()).contains("Dead-lettered");

        List<Object> retried = pollAll(retryConsumer, Duration.ofSeconds(5));
        assertThat(retried)
                .anyMatch(value -> value instanceof OptimizationRequestEvent event
                        && event.jobId().equals(rejectedJobId));
    }

    private OptimizationAckEvent awaitAck(AckStatus status, UUID jobId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            for (var record : ackConsumer.poll(Duration.ofMillis(250))) {
                if (record.value() instanceof OptimizationAckEvent ack
                        && ack.jobId().equals(jobId) && ack.status() == status) {
                    return ack;
                }
            }
        }
        throw new AssertionError("No " + status + " ack for job " + jobId + " within " + timeout);
    }

    private List<Object> pollAll(Consumer<String, Object> consumer, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<Object> values = new ArrayList<>();
        while (System.nanoTime() < deadline) {
            for (var record : consumer.poll(Duration.ofMillis(200))) {
                values.add(record.value());
            }
            if (!values.isEmpty()) {
                // Drain anything else that arrived in the same window.
                for (var record : consumer.poll(Duration.ofMillis(100))) {
                    values.add(record.value());
                }
                return values;
            }
        }
        return values;
    }

    private OptimizationRequestEvent request(UUID jobId) {
        return new OptimizationRequestEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION,
                jobId,
                1L,
                "saturation",
                List.of(),
                List.of(),
                Instant.now());
    }
}
