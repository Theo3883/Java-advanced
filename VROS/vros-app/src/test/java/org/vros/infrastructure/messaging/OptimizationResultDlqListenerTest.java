package org.vros.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.vros.application.service.OptimizationService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OptimizationResultDlqListenerTest {

    private final OptimizationService service = mock(OptimizationService.class);
    private final OptimizationMetrics metrics = mock(OptimizationMetrics.class);
    private final OptimizationResultDlqListener listener =
            new OptimizationResultDlqListener(service, metrics);

    @Test
    void marksJobFailedWhenResultIsDeadLettered() {
        UUID jobId = UUID.randomUUID();
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>(
                "vros.optimization.results-dlt", 0, 3L, jobId.toString(),
                "unreadable".getBytes(StandardCharsets.UTF_8));
        record.headers().add(new RecordHeader("kafka_dlt-exception-message",
                "db down".getBytes(StandardCharsets.UTF_8)));

        listener.onDeadLetter(record);

        verify(metrics).deadLettered();
        verify(service).markDeadLettered(eq(jobId), contains("db down"));
    }

    @Test
    void usesRecordKeyWhenPayloadIsUnreadable() {
        UUID jobId = UUID.randomUUID();
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>(
                "vros.optimization.results-dlt", 0, 4L, jobId.toString(), null);

        listener.onDeadLetter(record);

        verify(service).markDeadLettered(eq(jobId), contains("unknown error"));
    }
}
