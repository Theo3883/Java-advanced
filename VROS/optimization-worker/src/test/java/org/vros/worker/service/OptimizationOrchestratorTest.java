package org.vros.worker.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.worker.exception.UnsupportedSchemaVersionException;
import org.vros.worker.exception.WorkerBusyException;
import org.vros.worker.metrics.WorkerMetrics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OptimizationOrchestratorTest {

    private OptimizationTaskService taskService;
    private WorkerEventPublisher publisher;
    private JobDedupCache cache;
    private OptimizationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        taskService = mock(OptimizationTaskService.class);
        publisher = mock(WorkerEventPublisher.class);
        cache = new JobDedupCache();
        orchestrator = new OptimizationOrchestrator(
                taskService, publisher, cache,
                new WorkerMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void submitsTaskAndAcknowledgesAccepted() {
        OptimizationRequestEvent request = request(OptimizationRequestEvent.CURRENT_SCHEMA_VERSION);

        orchestrator.handle(request);

        verify(taskService).solveAsync(request);
        verify(publisher).publishAck(request.jobId(), AckStatus.ACCEPTED, "Optimization accepted");
    }

    @Test
    void rejectsUnsupportedSchemaWithoutSubmitting() {
        OptimizationRequestEvent request = request(999);

        assertThatThrownBy(() -> orchestrator.handle(request))
                .isInstanceOf(UnsupportedSchemaVersionException.class);
        verify(taskService, never()).solveAsync(any());
    }

    @Test
    void rePublishesResultForDuplicateCompletedJob() {
        OptimizationRequestEvent request = request(OptimizationRequestEvent.CURRENT_SCHEMA_VERSION);
        OptimizationResultEvent result = new OptimizationResultEvent(
                1, request.jobId(), 1L, "GREEDY_NEAREST_NEIGHBOR", 1.0, 1, true,
                List.of(), List.of(), Instant.now());
        cache.markCompleted(result);

        orchestrator.handle(request);

        verify(publisher).publishResult(result);
        verify(publisher).publishAck(request.jobId(), AckStatus.COMPLETED, "Optimization completed");
        verify(taskService, never()).solveAsync(any());
    }

    @Test
    void skipsDuplicateWhileJobIsInFlight() {
        OptimizationRequestEvent request = request(OptimizationRequestEvent.CURRENT_SCHEMA_VERSION);
        cache.markInProgress(request.jobId());

        orchestrator.handle(request);

        verify(taskService, never()).solveAsync(any());
        verify(publisher).publishAck(request.jobId(), AckStatus.ACCEPTED, "Optimization already running");
    }

    @Test
    void rejectsAndSignalsRetryWhenPoolIsSaturated() {
        OptimizationRequestEvent request = request(OptimizationRequestEvent.CURRENT_SCHEMA_VERSION);
        doThrow(new TaskRejectedException("pool full")).when(taskService).solveAsync(request);

        assertThatThrownBy(() -> orchestrator.handle(request))
                .isInstanceOf(WorkerBusyException.class);
        verify(publisher).publishAck(request.jobId(), AckStatus.REJECTED, "Worker busy");
        verify(publisher, never()).publishAck(request.jobId(), AckStatus.ACCEPTED, "Optimization accepted");
    }

    private OptimizationRequestEvent request(int schemaVersion) {
        return new OptimizationRequestEvent(
                schemaVersion,
                UUID.randomUUID(),
                1L,
                "scenario",
                List.of(),
                List.of(),
                Instant.now());
    }
}
