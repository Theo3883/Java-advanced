package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizationAckEvent(
        int schemaVersion,
        UUID jobId,
        AckStatus status,
        String message,
        Instant at) {
}
