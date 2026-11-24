package org.vros.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.vros.domain.model.OptimizationJobStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "optimization_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationJobEntity {

    @Id
    private UUID id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OptimizationJobStatus status;

    @Column(nullable = false)
    private int progress;

    @Column(length = 64)
    private String phase;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private int attempts;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
