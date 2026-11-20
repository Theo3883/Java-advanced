package org.vros.worker.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.worker.config.WorkerProperties;
import org.vros.worker.metrics.WorkerMetrics;
import org.vros.worker.solver.VrpSolver;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OptimizationTaskServiceTest {

    private VrpSolver solver;
    private WorkerEventPublisher publisher;
    private WorkerProperties properties;
    private OptimizationTaskService taskService;

    @BeforeEach
    void setUp() {
        solver = mock(VrpSolver.class);
        publisher = mock(WorkerEventPublisher.class);
        properties = new WorkerProperties();
        properties.setMaxSolverAttempts(3);
        properties.setRetryBackoffMs(1);
        properties.setSimulatedDelayMs(0);
        taskService = new OptimizationTaskService(
                solver, publisher, properties,
                new WorkerMetrics(new SimpleMeterRegistry()), new JobDedupCache());
    }

    @Test
    void publishesResultAndCompletedAckOnSuccess() {
        OptimizationRequestEvent request = request();
        OptimizationResultEvent result = result(request.jobId());
        when(solver.solve(any(), any())).thenReturn(result);

        taskService.solveAsync(request);

        verify(publisher).publishResult(result);
        verify(publisher).publishAck(request.jobId(), AckStatus.COMPLETED, "Optimization completed");
        verify(publisher, never()).publishToDlt(any(), any());
    }

    @Test
    void retriesTransientSolverFailures() {
        OptimizationRequestEvent request = request();
        OptimizationResultEvent result = result(request.jobId());
        when(solver.solve(any(), any()))
                .thenThrow(new IllegalStateException("transient-1"))
                .thenThrow(new IllegalStateException("transient-2"))
                .thenReturn(result);

        taskService.solveAsync(request);

        verify(solver, times(3)).solve(any(), any());
        verify(publisher).publishResult(result);
        verify(publisher, never()).publishToDlt(any(), any());
    }

    @Test
    void publishesFailedAckAndDltWhenRetriesAreExhausted() {
        OptimizationRequestEvent request = request();
        when(solver.solve(any(), any())).thenThrow(new IllegalStateException("always broken"));

        taskService.solveAsync(request);

        verify(solver, times(3)).solve(any(), any());
        verify(publisher).publishAck(eq(request.jobId()), eq(AckStatus.FAILED), any());
        verify(publisher).publishToDlt(eq(request), any());
        verify(publisher, never()).publishResult(any());
    }

    private OptimizationRequestEvent request() {
        return new OptimizationRequestEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID(),
                1L,
                "scenario",
                List.of(),
                List.of(),
                Instant.now());
    }

    private OptimizationResultEvent result(UUID jobId) {
        return new OptimizationResultEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION,
                jobId,
                1L,
                "GREEDY_NEAREST_NEIGHBOR",
                10.0,
                5,
                true,
                List.of(),
                List.of(),
                Instant.now());
    }
}
