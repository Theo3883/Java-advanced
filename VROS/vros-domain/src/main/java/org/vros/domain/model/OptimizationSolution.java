package org.vros.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationSolution {

    private UUID jobId;
    private double totalDistanceKm;
    private long computationMillis;
    private String algorithm;
    private boolean feasible;
    @Builder.Default
    private List<Long> unassignedDeliveryIds = new ArrayList<>();
    @Builder.Default
    private List<OptimizationRoute> routes = new ArrayList<>();
    private Instant createdAt;
}
