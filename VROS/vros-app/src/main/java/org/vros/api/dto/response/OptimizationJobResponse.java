package org.vros.api.dto.response;

import lombok.Builder;
import lombok.Data;
import org.vros.domain.model.OptimizationJobStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OptimizationJobResponse {
    private UUID jobId;
    private Long scenarioId;
    private OptimizationJobStatus status;
    private int progress;
    private String phase;
    private String error;
    private int attempts;
    private Instant submittedAt;
    private Instant updatedAt;
    private Instant completedAt;
}
