package org.vros.domain.port;

import org.vros.domain.model.OptimizationSolution;

import java.util.Optional;
import java.util.UUID;

public interface OptimizationSolutionRepository {

    Optional<OptimizationSolution> findByJobId(UUID jobId);
}
