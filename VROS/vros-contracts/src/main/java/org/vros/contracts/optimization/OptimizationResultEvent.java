package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizationResultEvent(
        int schemaVersion,
        UUID jobId,
        Long scenarioId,
        String algorithm,
        double totalDistanceKm,
        long computationMillis,
        boolean feasible,
        List<Long> unassignedDeliveryIds,
        List<RoutePlan> routes,
        Instant completedAt) {
}
