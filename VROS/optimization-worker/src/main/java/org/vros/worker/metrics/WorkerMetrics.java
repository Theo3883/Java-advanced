package org.vros.worker.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WorkerMetrics {

    private final MeterRegistry registry;
    private final Counter submitted;
    private final Counter completed;
    private final Counter progressEvents;
    private final Timer solveDuration;
    private final AtomicInteger inflight = new AtomicInteger();

    public WorkerMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.submitted = Counter.builder("vros.optimization.jobs.submitted")
                .description("Requests accepted by the worker")
                .register(registry);
        this.completed = Counter.builder("vros.optimization.jobs.completed")
                .description("Optimization jobs that produced a result")
                .register(registry);
        this.progressEvents = Counter.builder("vros.optimization.progress.events")
                .description("Progress events published to Kafka")
                .register(registry);
        this.solveDuration = Timer.builder("vros.optimization.solve.duration")
                .description("End-to-end solver task duration")
                .register(registry);
        Gauge.builder("vros.optimization.inflight", inflight, AtomicInteger::get)
                .description("Solver tasks currently executing")
                .register(registry);
    }

    public void jobSubmitted() {
        submitted.increment();
    }

    public void jobCompleted(long durationMillis) {
        completed.increment();
        solveDuration.record(java.time.Duration.ofMillis(durationMillis));
    }

    public void jobFailed(String reason) {
        Counter.builder("vros.optimization.jobs.failed")
                .description("Optimization jobs that failed")
                .tag("reason", reason == null ? "unknown" : reason)
                .register(registry)
                .increment();
    }

    public void jobRetried() {
        Counter.builder("vros.optimization.jobs.retried")
                .description("Solver or listener retries")
                .register(registry)
                .increment();
    }

    public void jobDeadLettered() {
        Counter.builder("vros.optimization.jobs.dead_lettered")
                .description("Requests routed to the dead-letter topic")
                .register(registry)
                .increment();
    }

    public void progressPublished() {
        progressEvents.increment();
    }

    public void inflightStarted() {
        inflight.incrementAndGet();
    }

    public void inflightFinished() {
        inflight.decrementAndGet();
    }
}
