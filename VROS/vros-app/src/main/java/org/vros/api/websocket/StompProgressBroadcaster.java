package org.vros.api.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.vros.application.port.OptimizationProgressNotifier;
import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.domain.model.OptimizationJob;

import java.time.Instant;

/**
 * Broadcasts optimization updates over STOMP.
 *
 * <p>Marked {@link Primary} so it wins over the no-op notifier registered by the infrastructure
 * module for non-web slices.</p>
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class StompProgressBroadcaster implements OptimizationProgressNotifier {

    public static final String JOB_TOPIC_PREFIX = "/topic/optimization/";
    public static final String GLOBAL_TOPIC = "/topic/optimization";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyProgress(OptimizationProgressEvent event) {
        broadcast(new OptimizationNotification(
                event.jobId(),
                "RUNNING",
                event.progress(),
                event.phase(),
                event.message(),
                event.at() == null ? Instant.now() : event.at()));
    }

    @Override
    public void notifyTerminal(OptimizationJob job) {
        broadcast(new OptimizationNotification(
                job.getId(),
                job.getStatus() == null ? "UNKNOWN" : job.getStatus().name(),
                job.getProgress(),
                job.getPhase(),
                job.getErrorMessage(),
                job.getUpdatedAt() == null ? Instant.now() : job.getUpdatedAt()));
    }

    private void broadcast(OptimizationNotification notification) {
        messagingTemplate.convertAndSend(JOB_TOPIC_PREFIX + notification.jobId(), notification);
        messagingTemplate.convertAndSend(GLOBAL_TOPIC, notification);
        log.debug("Broadcast {} for job {}", notification.status(), notification.jobId());
    }
}
