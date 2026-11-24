package org.vros.api.websocket;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload pushed to STOMP subscribers on {@code /topic/optimization/{jobId}}.
 */
public record OptimizationNotification(
        UUID jobId,
        String status,
        int progress,
        String phase,
        String message,
        Instant at) {
}
