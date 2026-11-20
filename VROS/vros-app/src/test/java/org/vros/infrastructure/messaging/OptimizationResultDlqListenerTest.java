package org.vros.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.vros.application.service.OptimizationService;
import org.vros.contracts.optimization.OptimizationResultEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OptimizationResultDlqListenerTest {

    private final OptimizationService service = mock(OptimizationService.class);
    private final OptimizationResultDlqListener listener = new OptimizationResultDlqListener(service);

    @Test
    void marksJobFailedWhenResultIsDeadLettered() {
        UUID jobId = UUID.randomUUID();
        ConsumerRecord<String, OptimizationResultEvent> record = new ConsumerRecord<>(
                "vros.optimization.results-dlt", 0, 3L, jobId.toString(),
                new OptimizationResultEvent(1, jobId, 1L, "GREEDY_NEAREST_NEIGHBOR",
                        1.0, 1, true, List.of(), List.of(), Instant.now()));
        record.headers().add(new RecordHeader("kafka_dlt-exception-message",
                "db down".getBytes(StandardCharsets.UTF_8)));

        listener.onDeadLetter(record);

        verify(service).markDeadLettered(eq(jobId), contains("db down"));
    }

    @Test
    void usesRecordKeyWhenPayloadIsUnreadable() {
        UUID jobId = UUID.randomUUID();
        ConsumerRecord<String, OptimizationResultEvent> record = new ConsumerRecord<>(
                "vros.optimization.results-dlt", 0, 4L, jobId.toString(), null);

        listener.onDeadLetter(record);

        verify(service).markDeadLettered(eq(jobId), contains("unknown error"));
    }
}
