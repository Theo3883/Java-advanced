package org.vros.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.application.service.OptimizationService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Handles results that VROS could not persist after retries (e.g. a mapping/DB failure).
 * Marks the corresponding job FAILED so clients stop polling forever.
 *
 * <p>The value is consumed as raw bytes because a dead-lettered payload may not be deserializable;
 * the job id comes from the record key.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationResultDlqListener {

    // Spring Kafka 4.x uses kafka_exception-*; older releases use kafka_dlt-exception-*.
    private static final String[] REASON_HEADERS = {
            "kafka_exception-message", "kafka_dlt-exception-message",
            "kafka_exception-fqcn", "kafka_dlt-exception-fqcn"};
    private static final String[] ORIGINAL_TOPIC_HEADERS = {
            "kafka_original-topic", "kafka_dlt-original-topic"};

    private final OptimizationService optimizationService;
    private final OptimizationMetrics metrics;

    @KafkaListener(
            topics = "${vros.kafka.topics.results-dlt}",
            groupId = "${vros.kafka.groups.api-dlq}",
            containerFactory = "resultsDltListenerContainerFactory")
    public void onDeadLetter(ConsumerRecord<String, byte[]> record) {
        UUID jobId = parseUuid(record.key());
        String reason = firstHeader(record, REASON_HEADERS);
        String originalTopic = firstHeader(record, ORIGINAL_TOPIC_HEADERS);
        log.error("Dead-lettered result topic={} partition={} offset={} key={} jobId={} originalTopic={} reason={}",
                record.topic(), record.partition(), record.offset(), record.key(), jobId, originalTopic, reason);
        metrics.deadLettered();
        if (jobId != null) {
            optimizationService.markDeadLettered(jobId,
                    "Result dead-lettered after retries: " + (reason == null ? "unknown error" : reason));
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
