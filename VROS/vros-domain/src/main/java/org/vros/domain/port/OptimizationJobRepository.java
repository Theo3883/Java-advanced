package org.vros.domain.port;

import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationJobStatus;
import org.vros.domain.model.OptimizationSolution;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OptimizationJobRepository {

    OptimizationJob save(OptimizationJob job);

    Optional<OptimizationJob> findById(UUID id);

    List<OptimizationJob> findAll();

    List<OptimizationJob> findByScenarioId(Long scenarioId);

    List<OptimizationJob> findByStatus(OptimizationJobStatus status);

    void updateProgress(UUID id, int progress, String phase, Instant updatedAt);

    void markCompleted(UUID id, OptimizationSolution solution, Instant completedAt);

    void markFailed(UUID id, String error, OptimizationJobStatus finalStatus, Instant updatedAt);
}
