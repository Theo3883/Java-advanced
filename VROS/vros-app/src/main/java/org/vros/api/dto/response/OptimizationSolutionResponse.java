package org.vros.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OptimizationSolutionResponse {
    private UUID jobId;
    private Long scenarioId;
    private String algorithm;
    private double totalDistanceKm;
    private long computationMillis;
    private boolean feasible;
    private List<Long> unassignedDeliveryIds;
    private List<OptimizationRouteResponse> routes;
    private Instant completedAt;
}
