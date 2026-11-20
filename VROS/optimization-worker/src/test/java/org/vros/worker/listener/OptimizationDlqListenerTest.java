package org.vros.worker.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vros.contracts.optimization.AckStatus;
import org.vros.worker.metrics.WorkerMetrics;
import org.vros.worker.service.WorkerEventPublisher;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OptimizationDlqListenerTest {

    private WorkerEventPublisher publisher;
    private WorkerMetrics metrics;
    private OptimizationDlqListener listener;

    @BeforeEach
    void setUp() {
        publisher = mock(WorkerEventPublisher.class);
        metrics = mock(WorkerMetrics.class);
        listener = new OptimizationDlqListener(publisher, metrics);
    }

    @Test
    void publishesFailedAckForDeadLetteredPayload() {
        UUID jobId = UUID.randomUUID();
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>(
                "vros.optimization.requests-dlt", 0, 10L, jobId.toString(),
                "unreadable".getBytes(StandardCharsets.UTF_8));
        record.headers().add(new RecordHeader("kafka_dlt-exception-message",
                "boom".getBytes(StandardCharsets.UTF_8)));

        listener.onDeadLetter(record);

        verify(metrics).jobDeadLettered();
        verify(publisher).publishAck(eq(jobId), eq(AckStatus.FAILED), contains("boom"));
    }

    @Test
    void fallsBackToRecordKeyWhenPayloadIsUnreadable() {
        UUID jobId = UUID.randomUUID();
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>(
                "vros.optimization.requests-dlt", 0, 11L, jobId.toString(), null);

        listener.onDeadLetter(record);

        verify(metrics).jobDeadLettered();
        verify(publisher).publishAck(eq(jobId), eq(AckStatus.FAILED), any());
    }
}
