package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizationProgressEvent(
        int schemaVersion,
        UUID jobId,
        int progress,
        String phase,
        String message,
        Instant at) {
}
