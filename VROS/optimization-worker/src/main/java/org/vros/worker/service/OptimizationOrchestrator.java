package org.vros.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.worker.exception.UnsupportedSchemaVersionException;
import org.vros.worker.exception.WorkerBusyException;
import org.vros.worker.metrics.WorkerMetrics;

import java.util.Optional;

/**
 * Entry point for a request event.
 *
 * <p>Validates the schema, deduplicates redeliveries, hands the work to the async task service and
 * acknowledges. Returning quickly is what lets the worker keep consuming while solvers run.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationOrchestrator {

    private final OptimizationTaskService taskService;
    private final WorkerEventPublisher publisher;
    private final JobDedupCache dedupCache;
    private final WorkerMetrics metrics;

    public void handle(OptimizationRequestEvent request) {
        validate(request);

        Optional<OptimizationResultEvent> completed = dedupCache.completedResult(request.jobId());
        if (completed.isPresent()) {
            log.info("Duplicate delivery for completed job {}; re-publishing result", request.jobId());
            publisher.publishResult(completed.get());
            publisher.publishAck(request.jobId(), AckStatus.COMPLETED, "Optimization completed");
            return;
        }
        if (dedupCache.isInProgress(request.jobId())) {
            log.info("Duplicate delivery for in-flight job {}; acknowledging", request.jobId());
            publisher.publishAck(request.jobId(), AckStatus.ACCEPTED, "Optimization already running");
            return;
        }

        dedupCache.markInProgress(request.jobId());
        try {
            taskService.solveAsync(request);
        } catch (TaskRejectedException ex) {
            dedupCache.remove(request.jobId());
            publisher.publishAck(request.jobId(), AckStatus.REJECTED, "Worker busy");
            metrics.jobRetried();
            throw new WorkerBusyException(
                    "Solver pool rejected job " + request.jobId() + " (worker busy)", ex);
        }

        publisher.publishAck(request.jobId(), AckStatus.ACCEPTED, "Optimization accepted");
        metrics.jobSubmitted();
    }

    private void validate(OptimizationRequestEvent request) {
        if (request == null || request.jobId() == null) {
            throw new IllegalArgumentException("Optimization request and job id are required");
        }
        if (request.schemaVersion() != OptimizationRequestEvent.CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedSchemaVersionException(
                    request.schemaVersion(), OptimizationRequestEvent.CURRENT_SCHEMA_VERSION);
        }
    }
}
