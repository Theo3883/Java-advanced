package org.vros.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vros.application.service.OptimizationService;
import org.vros.contracts.optimization.OptimizationResultEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Handles results that VROS could not persist after retries (e.g. a mapping/DB failure).
 * Marks the corresponding job FAILED so clients stop polling forever.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationResultDlqListener {

    private final OptimizationService optimizationService;

    @KafkaListener(
            topics = "${vros.kafka.topics.results-dlt}",
            groupId = "${vros.kafka.groups.api-dlq}",
            containerFactory = "optimizationListenerContainerFactory")
    public void onDeadLetter(ConsumerRecord<String, OptimizationResultEvent> record) {
        UUID jobId = record.value() == null ? parseUuid(record.key()) : record.value().jobId();
        String reason = header(record, "kafka_dlt-exception-message");
        if (reason == null) {
            reason = header(record, "kafka_dlt-exception-fqcn");
        }
        log.error("Dead-lettered result topic={} partition={} offset={} key={} jobId={} reason={}",
                record.topic(), record.partition(), record.offset(), record.key(), jobId, reason);
        if (jobId != null) {
            optimizationService.markDeadLettered(jobId,
                    "Result dead-lettered after retries: " + (reason == null ? "unknown error" : reason));
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
