package org.vros.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationRoute;
import org.vros.domain.model.OptimizationSolution;
import org.vros.infrastructure.persistence.entity.OptimizationJobEntity;
import org.vros.infrastructure.persistence.entity.OptimizationSolutionEntity;

import java.util.ArrayList;
import java.util.List;

public final class OptimizationMapper {

    private static final TypeReference<List<OptimizationRoute>> ROUTES_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Long>> IDS_TYPE = new TypeReference<>() {
    };

    private OptimizationMapper() {
    }

    public static OptimizationJob toDomain(OptimizationJobEntity entity) {
        if (entity == null) {
            return null;
        }
        return OptimizationJob.builder()
                .id(entity.getId())
                .scenarioId(entity.getScenarioId())
                .status(entity.getStatus())
                .progress(entity.getProgress())
                .phase(entity.getPhase())
                .errorMessage(entity.getErrorMessage())
                .attempts(entity.getAttempts())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    public static OptimizationJobEntity toEntity(OptimizationJob job) {
        if (job == null) {
            return null;
        }
        return OptimizationJobEntity.builder()
                .id(job.getId())
                .scenarioId(job.getScenarioId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .phase(job.getPhase())
                .errorMessage(job.getErrorMessage())
                .attempts(job.getAttempts())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    public static OptimizationSolutionEntity toEntity(OptimizationSolution solution, ObjectMapper mapper) {
        if (solution == null) {
            return null;
        }
        return OptimizationSolutionEntity.builder()
                .jobId(solution.getJobId())
                .totalDistanceKm(solution.getTotalDistanceKm())
                .computationMillis(solution.getComputationMillis())
                .algorithm(solution.getAlgorithm())
                .feasible(solution.isFeasible())
                .unassignedDeliveryIds(write(mapper, solution.getUnassignedDeliveryIds()))
                .routesJson(write(mapper, solution.getRoutes()))
                .createdAt(solution.getCreatedAt())
                .build();
    }

    public static OptimizationSolution toDomain(OptimizationSolutionEntity entity, ObjectMapper mapper) {
        if (entity == null) {
            return null;
        }
        return OptimizationSolution.builder()
                .jobId(entity.getJobId())
                .totalDistanceKm(entity.getTotalDistanceKm())
                .computationMillis(entity.getComputationMillis())
                .algorithm(entity.getAlgorithm())
                .feasible(entity.isFeasible())
                .unassignedDeliveryIds(read(mapper, entity.getUnassignedDeliveryIds(), IDS_TYPE, new ArrayList<>()))
                .routes(read(mapper, entity.getRoutesJson(), ROUTES_TYPE, new ArrayList<>()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private static String write(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize optimization solution", e);
        }
    }

    private static <T> T read(ObjectMapper mapper, String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize optimization solution", e);
        }
    }
}
