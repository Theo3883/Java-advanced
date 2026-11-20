package org.vros.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.OptimizationAckEvent;
import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.worker.config.WorkerKafkaProperties;
import org.vros.worker.metrics.WorkerMetrics;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Publishes worker events to Kafka. All events are keyed by {@code jobId} so per-job ordering is
 * preserved inside each topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerEventPublisher {

    private static final String DLT_EXCEPTION_MESSAGE = "kafka_dlt-exception-message";
    private static final String DLT_EXCEPTION_FQCN = "kafka_dlt-exception-fqcn";
    private static final String DLT_ORIGINAL_TOPIC = "kafka_dlt-original-topic";

    private final KafkaTemplate<String, Object> optimizationKafkaTemplate;
    private final WorkerKafkaProperties properties;
    private final WorkerMetrics metrics;

    public void publishAck(UUID jobId, AckStatus status, String message) {
        OptimizationAckEvent event = new OptimizationAckEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION, jobId, status, message, Instant.now());
        send(properties.getTopics().getAcks(), jobId, event);
    }

    public void publishProgress(UUID jobId, int progress, String phase, String message) {
        OptimizationProgressEvent event = new OptimizationProgressEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION, jobId, progress, phase, message, Instant.now());
        send(properties.getTopics().getProgress(), jobId, event);
        metrics.progressPublished();
    }

    public void publishResult(OptimizationResultEvent event) {
        send(properties.getTopics().getResults(), event.jobId(), event);
    }

    public void publishToDlt(OptimizationRequestEvent request, Throwable error) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                properties.getTopics().getRequestsDlt(),
                request.jobId().toString(),
                request);
        record.headers()
                .add(new RecordHeader(DLT_EXCEPTION_MESSAGE,
                        bytes(error.getMessage() == null ? error.getClass().getName() : error.getMessage())))
                .add(new RecordHeader(DLT_EXCEPTION_FQCN, bytes(error.getClass().getName())))
                .add(new RecordHeader(DLT_ORIGINAL_TOPIC,
                        bytes(properties.getTopics().getRequests())));
        optimizationKafkaTemplate.send(record).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Could not publish job {} to the dead-letter topic", request.jobId(), throwable);
            }
        });
        metrics.jobDeadLettered();
    }

    private void send(String topic, UUID jobId, Object event) {
        optimizationKafkaTemplate.send(topic, jobId.toString(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Could not publish {} to {}", event.getClass().getSimpleName(), topic, throwable);
                    }
                });
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
