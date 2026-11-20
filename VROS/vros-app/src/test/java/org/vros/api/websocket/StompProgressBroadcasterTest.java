package org.vros.api.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationJobStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StompProgressBroadcasterTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final StompProgressBroadcaster broadcaster = new StompProgressBroadcaster(messagingTemplate);

    @Test
    void broadcastsProgressToJobAndGlobalTopics() {
        UUID jobId = UUID.randomUUID();
        Instant at = Instant.now();

        broadcaster.notifyProgress(new OptimizationProgressEvent(
                1, jobId, 45, "ASSIGNING_DELIVERIES", "Assigned 9 of 20 deliveries", at));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                eq(StompProgressBroadcaster.JOB_TOPIC_PREFIX + jobId), payload.capture());
        verify(messagingTemplate).convertAndSend(
                eq(StompProgressBroadcaster.GLOBAL_TOPIC), eq(payload.getValue()));

        OptimizationNotification notification = (OptimizationNotification) payload.getValue();
        assertThat(notification.jobId()).isEqualTo(jobId);
        assertThat(notification.status()).isEqualTo("RUNNING");
        assertThat(notification.progress()).isEqualTo(45);
        assertThat(notification.phase()).isEqualTo("ASSIGNING_DELIVERIES");
        assertThat(notification.at()).isEqualTo(at);
    }

    @Test
    void broadcastsTerminalStatus() {
        UUID jobId = UUID.randomUUID();
        OptimizationJob job = OptimizationJob.builder()
                .id(jobId)
                .status(OptimizationJobStatus.COMPLETED)
                .progress(100)
                .phase("COMPLETED")
                .updatedAt(Instant.now())
                .build();

        broadcaster.notifyTerminal(job);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2)).convertAndSend(org.mockito.ArgumentMatchers.anyString(), payload.capture());
        OptimizationNotification notification = (OptimizationNotification) payload.getValue();
        assertThat(notification.status()).isEqualTo("COMPLETED");
        assertThat(notification.progress()).isEqualTo(100);
    }
}
