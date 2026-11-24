package org.vros.infrastructure.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.AckStatus;

/**
 * Micrometer counters for the VROS side of the optimization flow.
 */
@Component
public class OptimizationMetrics {

    private final MeterRegistry registry;

    public OptimizationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void requestSubmitted(Long scenarioId) {
        counter("vros.optimization.jobs.submitted", "Request published to Kafka",
                "scenarioId", String.valueOf(scenarioId))
                .increment();
    }

    public void ackReceived(AckStatus status) {
        if (status == AckStatus.FAILED) {
            counter("vros.optimization.jobs.failed", "Jobs failed per ACK",
                    "reason", "ack").increment();
        } else if (status == AckStatus.REJECTED) {
            counter("vros.optimization.jobs.retried", "Jobs rejected by the worker",
                    "reason", "rejected").increment();
        }
    }

    public void resultCompleted() {
        counter("vros.optimization.jobs.completed", "Completed optimization jobs",
                null, null).increment();
    }

    public void progressEvent() {
        counter("vros.optimization.progress.events", "Progress events consumed from Kafka",
                null, null).increment();
    }

    public void jobFailed(String reason) {
        counter("vros.optimization.jobs.failed", "Jobs failed per reason",
                "reason", reason == null ? "unknown" : reason).increment();
    }

    public void deadLettered() {
        counter("vros.optimization.jobs.dead_lettered", "Results dead-lettered on the VROS side",
                null, null).increment();
    }

    private Counter counter(String name, String description, String tagKey, String tagValue) {
        Counter.Builder builder = Counter.builder(name).description(description);
        if (tagKey != null) {
            builder.tag(tagKey, tagValue);
        }
        return builder.register(registry);
    }
}
