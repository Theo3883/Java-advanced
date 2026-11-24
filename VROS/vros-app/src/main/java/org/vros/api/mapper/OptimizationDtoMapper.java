package org.vros.api.mapper;

import org.vros.api.dto.response.OptimizationJobResponse;
import org.vros.api.dto.response.OptimizationRouteResponse;
import org.vros.api.dto.response.OptimizationSolutionResponse;
import org.vros.api.dto.response.OptimizationStopResponse;
import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationRoute;
import org.vros.domain.model.OptimizationSolution;
import org.vros.domain.model.OptimizationStop;

public final class OptimizationDtoMapper {

    private OptimizationDtoMapper() {
    }

    public static OptimizationJobResponse toResponse(OptimizationJob job) {
        return OptimizationJobResponse.builder()
                .jobId(job.getId())
                .scenarioId(job.getScenarioId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .phase(job.getPhase())
                .error(job.getErrorMessage())
                .attempts(job.getAttempts())
                .submittedAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    public static OptimizationSolutionResponse toResponse(OptimizationSolution solution) {
        return OptimizationSolutionResponse.builder()
                .jobId(solution.getJobId())
                .algorithm(solution.getAlgorithm())
                .totalDistanceKm(solution.getTotalDistanceKm())
                .computationMillis(solution.getComputationMillis())
                .feasible(solution.isFeasible())
                .unassignedDeliveryIds(solution.getUnassignedDeliveryIds())
                .routes(solution.getRoutes().stream().map(OptimizationDtoMapper::toResponse).toList())
                .completedAt(solution.getCreatedAt())
                .build();
    }

    private static OptimizationRouteResponse toResponse(OptimizationRoute route) {
        return OptimizationRouteResponse.builder()
                .vehicleId(route.getVehicleId())
                .vehicleName(route.getVehicleName())
                .load(route.getLoad())
                .distanceKm(route.getDistanceKm())
                .stops(route.getStops().stream().map(OptimizationDtoMapper::toResponse).toList())
                .build();
    }

    private static OptimizationStopResponse toResponse(OptimizationStop stop) {
        return OptimizationStopResponse.builder()
                .sequence(stop.getSequence())
                .deliveryId(stop.getDeliveryId())
                .lat(stop.getLat())
                .lng(stop.getLng())
                .quantity(stop.getQuantity())
                .legDistanceKm(stop.getLegDistanceKm())
                .build();
    }
}
