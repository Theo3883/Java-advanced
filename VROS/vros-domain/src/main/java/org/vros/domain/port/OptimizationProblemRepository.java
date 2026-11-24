package org.vros.domain.port;

import org.vros.domain.model.OptimizationProblem;

import java.util.Optional;

public interface OptimizationProblemRepository {

    Optional<OptimizationProblem> findByScenarioId(Long scenarioId);
}
