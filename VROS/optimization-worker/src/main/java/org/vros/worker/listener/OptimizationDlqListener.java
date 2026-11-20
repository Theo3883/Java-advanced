package org.vros.worker.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.worker.metrics.WorkerMetrics;
import org.vros.worker.service.WorkerEventPublisher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Terminal handler for the worker dead-letter topic.
 *
 * <p>Not annotated with {@code @RetryableTopic}: a DLT must stay dead. The listener publishes a
 * FAILED ack so VROS can mark the job failed, and increments the dead-letter metric.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationDlqListener {

    private final WorkerEventPublisher publisher;
    private final WorkerMetrics metrics;

    @KafkaListener(
            topics = "${vros.kafka.topics.requests-dlt}",
            groupId = "${vros.kafka.groups.worker-dlq}")
    public void onDeadLetter(ConsumerRecord<String, OptimizationRequestEvent> record) {
        UUID jobId = record.value() == null ? parseUuid(record.key()) : record.value().jobId();
        String reason = header(record, "kafka_dlt-exception-message");
        if (reason == null) {
            reason = header(record, "kafka_dlt-exception-fqcn");
        }
        log.error("Dead-lettered optimization request topic={} partition={} offset={} key={} jobId={} reason={}",
                record.topic(), record.partition(), record.offset(), record.key(), jobId, reason);
        metrics.jobDeadLettered();
        if (jobId != null) {
            publisher.publishAck(jobId, AckStatus.FAILED,
                    "Dead-lettered after retries: " + (reason == null ? "unknown error" : reason));
        }
    }

    private String header(ConsumerRecord<?, ?> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null || header.value() == null
                ? null
                : new String(header.value(), StandardCharsets.UTF_8);
    }

    private UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
