package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.port.OptimizationEventPublisher;
import org.vros.application.port.OptimizationProgressNotifier;
import org.vros.application.service.OptimizationService;
import org.vros.contracts.optimization.AckStatus;
import org.vros.contracts.optimization.DeliverySpec;
import org.vros.contracts.optimization.OptimizationAckEvent;
import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.contracts.optimization.RoutePlan;
import org.vros.contracts.optimization.RouteStop;
import org.vros.contracts.optimization.VehicleSpec;
import org.vros.domain.exception.InvalidOptimizationProblemException;
import org.vros.domain.exception.OptimizationJobNotReadyException;
import org.vros.domain.exception.OptimizationSubmissionException;
import org.vros.domain.exception.ResourceNotFoundException;
import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationJobStatus;
import org.vros.domain.model.OptimizationProblem;
import org.vros.domain.model.OptimizationRoute;
import org.vros.domain.model.OptimizationSolution;
import org.vros.domain.model.OptimizationStop;
import org.vros.domain.model.ProblemDelivery;
import org.vros.domain.model.ProblemVehicle;
import org.vros.domain.port.OptimizationJobRepository;
import org.vros.domain.port.OptimizationProblemRepository;
import org.vros.domain.port.OptimizationSolutionRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class OptimizationServiceImpl implements OptimizationService {

    private final OptimizationJobRepository jobRepository;
    private final OptimizationSolutionRepository solutionRepository;
    private final OptimizationProblemRepository problemRepository;
    private final OptimizationEventPublisher eventPublisher;
    private final OptimizationProgressNotifier progressNotifier;

    @Override
    public OptimizationJob submit(Long scenarioId) {
        OptimizationProblem problem = problemRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", scenarioId));
        validate(problem);

        Instant now = Instant.now();
        OptimizationJob job = OptimizationJob.builder()
                .id(UUID.randomUUID())
                .scenarioId(problem.getScenarioId())
                .status(OptimizationJobStatus.PENDING)
                .progress(0)
                .phase("SUBMITTED")
                .attempts(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        job = jobRepository.save(job);

        try {
            eventPublisher.publishRequest(toRequest(problem, job.getId(), now));
            job.setStatus(OptimizationJobStatus.QUEUED);
            job.setPhase("QUEUED");
            job.setUpdatedAt(Instant.now());
            return jobRepository.save(job);
        } catch (RuntimeException ex) {
            job.setStatus(OptimizationJobStatus.SUBMISSION_FAILED);
            job.setPhase("SUBMISSION_FAILED");
            job.setErrorMessage(rootMessage(ex));
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            throw new OptimizationSubmissionException(
                    "Could not publish optimization request for scenario " + scenarioId, ex);
        }
    }

    @Override
    public OptimizationJob getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Optimization job " + jobId));
    }

    @Override
    public OptimizationSolution getSolution(UUID jobId) {
        OptimizationJob job = getJob(jobId);
        if (job.getStatus() != OptimizationJobStatus.COMPLETED) {
            throw new OptimizationJobNotReadyException(jobId, job.getStatus().name());
        }
        return solutionRepository.findByJobId(jobId)
                .orElseThrow(() -> new OptimizationJobNotReadyException(jobId, job.getStatus().name()));
    }

    @Override
    public List<OptimizationJob> list(Long scenarioId, OptimizationJobStatus status) {
        List<OptimizationJob> jobs;
        if (scenarioId != null) {
            jobs = jobRepository.findByScenarioId(scenarioId);
        } else if (status != null) {
            jobs = jobRepository.findByStatus(status);
        } else {
            jobs = jobRepository.findAll();
        }
        if (scenarioId != null && status != null) {
            jobs = jobs.stream().filter(j -> j.getStatus() == status).toList();
        }
        return jobs.stream()
                .sorted(Comparator.comparing(OptimizationJob::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public void handleAck(OptimizationAckEvent event) {
        Optional<OptimizationJob> maybeJob = jobRepository.findById(event.jobId());
        if (maybeJob.isEmpty()) {
            return;
        }
        OptimizationJob job = maybeJob.get();
        if (job.getStatus().isTerminal()) {
            return;
        }

        switch (event.status()) {
            case ACCEPTED -> {
                if (job.getStatus() != OptimizationJobStatus.RUNNING) {
                    job.setStatus(OptimizationJobStatus.ACCEPTED);
                    job.setPhase("ACCEPTED");
                }
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
            }
            case REJECTED -> {
                job.setStatus(OptimizationJobStatus.RETRYING);
                job.setPhase("RETRYING");
                job.setErrorMessage(event.message());
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
            }
            case RUNNING -> {
                job.setStatus(OptimizationJobStatus.RUNNING);
                job.setPhase("RUNNING");
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
            }
            case FAILED -> jobRepository.markFailed(
                    job.getId(), event.message(), OptimizationJobStatus.FAILED, Instant.now());
            case COMPLETED -> {
                // The result event is the source of truth for COMPLETED.
            }
        }
    }

    @Override
    public void handleProgress(OptimizationProgressEvent event) {
        Optional<OptimizationJob> maybeJob = jobRepository.findById(event.jobId());
        if (maybeJob.isEmpty() || maybeJob.get().getStatus().isTerminal()) {
            return;
        }
        // 100 % is owned by the result event. Accepting it here would race with markCompleted.
        if (event.progress() >= 100) {
            progressNotifier.notifyProgress(event);
            return;
        }
        jobRepository.updateProgress(event.jobId(), event.progress(), event.phase(), Instant.now());
        progressNotifier.notifyProgress(event);
    }

    @Override
    public void handleResult(OptimizationResultEvent event) {
        if (solutionRepository.findByJobId(event.jobId()).isPresent()) {
            return;
        }
        OptimizationSolution solution = toSolution(event);
        jobRepository.markCompleted(event.jobId(), solution, Instant.now());
        jobRepository.findById(event.jobId()).ifPresent(progressNotifier::notifyTerminal);
    }

    @Override
    public void markDeadLettered(UUID jobId, String reason) {
        Optional<OptimizationJob> maybeJob = jobRepository.findById(jobId);
        if (maybeJob.isEmpty() || maybeJob.get().getStatus() == OptimizationJobStatus.COMPLETED) {
            return;
        }
        jobRepository.markFailed(jobId, reason, OptimizationJobStatus.FAILED, Instant.now());
        jobRepository.findById(jobId).ifPresent(progressNotifier::notifyTerminal);
    }

    private void validate(OptimizationProblem problem) {
        if (problem.getScenarioId() == null) {
            throw new InvalidOptimizationProblemException("Scenario id is required");
        }
        boolean hasUsableVehicle = problem.getVehicles().stream()
                .anyMatch(v -> v.isAvailable() && v.getCapacity() > 0 && v.getDepot() != null);
        if (!hasUsableVehicle) {
            throw new InvalidOptimizationProblemException(
                    "Scenario " + problem.getScenarioId()
                            + " has no available vehicle with capacity and depot coordinates");
        }
        if (problem.getDeliveries().isEmpty()) {
            throw new InvalidOptimizationProblemException(
                    "Scenario " + problem.getScenarioId() + " has no deliveries");
        }
        for (ProblemDelivery delivery : problem.getDeliveries()) {
            if (delivery.getQuantity() <= 0) {
                throw new InvalidOptimizationProblemException(
                        "Delivery " + delivery.getId() + " has a non-positive quantity");
            }
            if (delivery.getCoordinates() == null) {
                throw new InvalidOptimizationProblemException(
                        "Delivery " + delivery.getId() + " has no customer coordinates");
            }
        }
    }

    private OptimizationRequestEvent toRequest(OptimizationProblem problem, UUID jobId, Instant now) {
        List<VehicleSpec> vehicles = problem.getVehicles().stream()
                .filter(v -> v.isAvailable() && v.getCapacity() > 0 && v.getDepot() != null)
                .map(v -> new VehicleSpec(
                        v.getId(), v.getName(), v.getCapacity(),
                        v.getDepot().getLat(), v.getDepot().getLng()))
                .toList();
        List<DeliverySpec> deliveries = problem.getDeliveries().stream()
                .map(d -> new DeliverySpec(
                        d.getId(), d.getCustomerId(), d.getQuantity(),
                        d.getCoordinates().getLat(), d.getCoordinates().getLng()))
                .toList();
        return new OptimizationRequestEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION,
                jobId,
                problem.getScenarioId(),
                problem.getScenarioName(),
                vehicles,
                deliveries,
                now);
    }

    private OptimizationSolution toSolution(OptimizationResultEvent event) {
        List<OptimizationRoute> routes = event.routes() == null ? List.of() : event.routes().stream()
                .map(this::toRoute)
                .toList();
        return OptimizationSolution.builder()
                .jobId(event.jobId())
                .totalDistanceKm(event.totalDistanceKm())
                .computationMillis(event.computationMillis())
                .algorithm(event.algorithm())
                .feasible(event.feasible())
                .unassignedDeliveryIds(event.unassignedDeliveryIds() == null
                        ? List.of() : event.unassignedDeliveryIds())
                .routes(routes)
                .createdAt(event.completedAt() == null ? Instant.now() : event.completedAt())
                .build();
    }

    private OptimizationRoute toRoute(RoutePlan plan) {
        List<OptimizationStop> stops = plan.stops() == null ? List.of() : plan.stops().stream()
                .map(this::toStop)
                .toList();
        return OptimizationRoute.builder()
                .vehicleId(plan.vehicleId())
                .vehicleName(plan.vehicleName())
                .load(plan.load())
                .distanceKm(plan.distanceKm())
                .stops(stops)
                .build();
    }

    private OptimizationStop toStop(RouteStop stop) {
        return OptimizationStop.builder()
                .sequence(stop.sequence())
                .deliveryId(stop.deliveryId())
                .lat(stop.lat())
                .lng(stop.lng())
                .quantity(stop.quantity())
                .legDistanceKm(stop.legDistanceKm())
                .build();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
