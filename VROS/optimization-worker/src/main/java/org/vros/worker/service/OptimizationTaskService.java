package org.vros.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.worker.config.AsyncConfig;
import org.vros.worker.config.WorkerProperties;
import org.vros.worker.metrics.WorkerMetrics;
import org.vros.worker.solver.VrpSolver;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Runs the solver on the bounded executor.
 *
 * <p>The Kafka listener returns as soon as the task is submitted, so the offset is committed and
 * the worker keeps consuming. Because Kafka can no longer retry an exception thrown after that
 * point, solver failures are retried inside the task and, when exhausted, are published to the
 * dead-letter topic together with a FAILED ack.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationTaskService {

    private final VrpSolver solver;
    private final WorkerEventPublisher publisher;
    private final WorkerProperties properties;
    private final WorkerMetrics metrics;
    private final JobDedupCache dedupCache;

    @Async(AsyncConfig.OPTIMIZATION_EXECUTOR)
    public CompletableFuture<Void> solveAsync(OptimizationRequestEvent request) {
        long startedAt = System.currentTimeMillis();
        UUID jobId = request.jobId();
        metrics.inflightStarted();
        try {
            ProgressThrottle throttle = new ProgressThrottle(jobId);
            throttle.publish(1, "STARTED", "Worker started");
            simulateDelay();

            OptimizationResultEvent result = solveWithRetry(request, throttle);
            throttle.publish(100, "COMPLETED", "Optimization completed");

            publisher.publishResult(result);
            publisher.publishAck(jobId, AckStatus.COMPLETED, "Optimization completed");
            dedupCache.markCompleted(result);
            metrics.jobCompleted(System.currentTimeMillis() - startedAt);
            log.info("Job {} completed in {} ms ({} routes, {} unassigned)",
                    jobId, System.currentTimeMillis() - startedAt,
                    result.routes().size(), result.unassignedDeliveryIds().size());
        } catch (Exception ex) {
            log.error("Job {} failed after in-task retries", jobId, ex);
            publisher.publishAck(jobId, AckStatus.FAILED, rootMessage(ex));
            publisher.publishToDlt(request, ex);
            dedupCache.remove(jobId);
            metrics.jobFailed("solver");
        } finally {
            metrics.inflightFinished();
        }
        return CompletableFuture.completedFuture(null);
    }

    private OptimizationResultEvent solveWithRetry(
            OptimizationRequestEvent request, ProgressThrottle throttle) {
        int attempts = Math.max(1, properties.getMaxSolverAttempts());
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return solver.solve(request, throttle::publish);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                metrics.jobRetried();
                log.warn("Solver attempt {}/{} failed for job {}", attempt, attempts, request.jobId(), ex);
                if (attempt < attempts) {
                    sleep(properties.getRetryBackoffMs() * attempt);
                }
            }
        }
        throw lastFailure == null
                ? new IllegalStateException("Solver failed without an exception")
                : lastFailure;
    }

    private void simulateDelay() {
        if (properties.getSimulatedDelayMs() > 0) {
            sleep(properties.getSimulatedDelayMs());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while solving", ex);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    /**
     * Per-task progress throttle. Progress is published when the percentage advances by at least
     * five points, when at least {@code progress-throttle-ms} elapsed, or for terminal events.
     */
    private final class ProgressThrottle {

        private final UUID jobId;
        private final long throttleMillis = properties.getProgressThrottleMs();
        private int lastProgress = -1;
        private long lastPublishedAt;

        private ProgressThrottle(UUID jobId) {
            this.jobId = jobId;
        }

        private void publish(int progress, String phase, String message) {
            long now = System.currentTimeMillis();
            boolean terminal = progress >= 100;
            boolean advanced = progress - lastProgress >= 5;
            boolean elapsed = now - lastPublishedAt >= throttleMillis;
            if (!terminal && !advanced && !elapsed) {
                return;
            }
            lastProgress = progress;
            lastPublishedAt = now;
            publisher.publishProgress(jobId, progress, phase, message);
        }
    }
}
