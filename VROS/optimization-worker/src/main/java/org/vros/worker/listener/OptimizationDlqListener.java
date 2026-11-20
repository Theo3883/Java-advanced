package org.vros.worker.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.AckStatus;
import org.vros.worker.metrics.WorkerMetrics;
import org.vros.worker.service.WorkerEventPublisher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Terminal handler for the worker dead-letter topic.
 *
 * <p>Not annotated with {@code @RetryableTopic}: a DLT must stay dead. The record value is read as
 * raw bytes because a dead-lettered payload may not be deserializable; the job id comes from the
 * record key, which VROS and the worker always set to {@code jobId}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationDlqListener {

    // Spring Kafka 4.x uses kafka_exception-*; older releases and our explicit DLT publisher
    // use kafka_dlt-exception-*. Accept both so reasons are preserved.
    private static final String[] REASON_HEADERS = {
            "kafka_exception-message", "kafka_dlt-exception-message",
            "kafka_exception-fqcn", "kafka_dlt-exception-fqcn"};
    private static final String[] ORIGINAL_TOPIC_HEADERS = {
            "kafka_original-topic", "kafka_dlt-original-topic"};

    private final WorkerEventPublisher publisher;
    private final WorkerMetrics metrics;

    @KafkaListener(
            topics = "${vros.kafka.topics.requests-dlt}",
            groupId = "${vros.kafka.groups.worker-dlq}",
            containerFactory = "dltListenerContainerFactory")
    public void onDeadLetter(ConsumerRecord<String, byte[]> record) {
        UUID jobId = parseUuid(record.key());
        String reason = firstHeader(record, REASON_HEADERS);
        String originalTopic = firstHeader(record, ORIGINAL_TOPIC_HEADERS);
        log.error("Dead-lettered optimization request topic={} partition={} offset={} key={} jobId={} originalTopic={} reason={}",
                record.topic(), record.partition(), record.offset(), record.key(), jobId, originalTopic, reason);
        metrics.jobDeadLettered();
        if (jobId != null) {
            publisher.publishAck(jobId, AckStatus.FAILED,
                    "Dead-lettered after retries: " + (reason == null ? "unknown error" : reason));
        }
    }

    private String firstHeader(ConsumerRecord<?, ?> record, String[] names) {
        for (String name : names) {
            var header = record.headers().lastHeader(name);
            if (header != null && header.value() != null) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
        }
        return null;
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
