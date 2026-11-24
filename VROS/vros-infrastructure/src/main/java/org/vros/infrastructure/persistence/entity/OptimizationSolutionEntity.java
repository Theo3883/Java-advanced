package org.vros.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "optimization_solutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationSolutionEntity {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "total_distance_km", nullable = false)
    private double totalDistanceKm;

    @Column(name = "computation_millis", nullable = false)
    private long computationMillis;

    @Column(nullable = false, length = 64)
    private String algorithm;

    @Column(nullable = false)
    private boolean feasible;

    @Column(name = "unassigned_delivery_ids", columnDefinition = "TEXT")
    private String unassignedDeliveryIds;

    @Column(name = "routes_json", nullable = false, columnDefinition = "TEXT")
    private String routesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
