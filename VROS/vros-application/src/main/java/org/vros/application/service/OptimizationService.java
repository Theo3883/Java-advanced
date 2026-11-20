package org.vros.application.service;

import org.vros.contracts.optimization.OptimizationAckEvent;
import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationJobStatus;
import org.vros.domain.model.OptimizationSolution;

import java.util.List;
import java.util.UUID;

public interface OptimizationService {

    OptimizationJob submit(Long scenarioId);

    OptimizationJob getJob(UUID jobId);

    OptimizationSolution getSolution(UUID jobId);

    List<OptimizationJob> list(Long scenarioId, OptimizationJobStatus status);

    void handleAck(OptimizationAckEvent event);

    void handleProgress(OptimizationProgressEvent event);

    void handleResult(OptimizationResultEvent event);

    void markDeadLettered(UUID jobId, String reason);
}
