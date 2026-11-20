package org.vros.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationJob {

    private UUID id;
    private Long scenarioId;
    private OptimizationJobStatus status;
    private int progress;
    private String phase;
    private String errorMessage;
    private int attempts;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}
