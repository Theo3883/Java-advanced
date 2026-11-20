package org.vros.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vros.domain.model.OptimizationJob;
import org.vros.domain.model.OptimizationJobStatus;
import org.vros.domain.model.OptimizationSolution;
import org.vros.domain.port.OptimizationJobRepository;
import org.vros.infrastructure.persistence.entity.OptimizationJobEntity;
import org.vros.infrastructure.persistence.mapper.OptimizationMapper;
import org.vros.infrastructure.persistence.springdata.OptimizationJobJpaRepository;
import org.vros.infrastructure.persistence.springdata.OptimizationSolutionJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptimizationJobRepositoryAdapter implements OptimizationJobRepository {

    private static final List<OptimizationJobStatus> TERMINAL = List.of(
            OptimizationJobStatus.COMPLETED,
            OptimizationJobStatus.FAILED,
            OptimizationJobStatus.SUBMISSION_FAILED);

    private final OptimizationJobJpaRepository jobJpaRepository;
    private final OptimizationSolutionJpaRepository solutionJpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OptimizationJob save(OptimizationJob job) {
        OptimizationJobEntity entity = job.getId() == null
                ? OptimizationMapper.toEntity(job)
                : jobJpaRepository.findById(job.getId())
                        .orElseGet(() -> OptimizationMapper.toEntity(job));
        copyFields(job, entity);
        return OptimizationMapper.toDomain(jobJpaRepository.save(entity));
    }

    @Override
    public Optional<OptimizationJob> findById(UUID id) {
        return jobJpaRepository.findById(id).map(OptimizationMapper::toDomain);
    }

    @Override
    public List<OptimizationJob> findAll() {
        return jobJpaRepository.findAll().stream().map(OptimizationMapper::toDomain).toList();
    }

    @Override
    public List<OptimizationJob> findByScenarioId(Long scenarioId) {
        return jobJpaRepository.findByScenarioId(scenarioId).stream()
                .map(OptimizationMapper::toDomain).toList();
    }

    @Override
    public List<OptimizationJob> findByStatus(OptimizationJobStatus status) {
        return jobJpaRepository.findByStatus(status).stream()
                .map(OptimizationMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void updateProgress(UUID id, int progress, String phase, Instant updatedAt) {
        jobJpaRepository.updateProgress(id, progress, phase, OptimizationJobStatus.RUNNING,
                updatedAt, TERMINAL);
    }

    @Override
    @Transactional
    public void markCompleted(UUID id, OptimizationSolution solution, Instant completedAt) {
        OptimizationJobEntity entity = jobJpaRepository.findById(id).orElse(null);
        if (entity == null || entity.getStatus() == OptimizationJobStatus.COMPLETED) {
            return;
        }
        entity.setStatus(OptimizationJobStatus.COMPLETED);
        entity.setProgress(100);
        entity.setPhase("COMPLETED");
        entity.setErrorMessage(null);
        entity.setCompletedAt(completedAt);
        entity.setUpdatedAt(completedAt);
        jobJpaRepository.save(entity);
        solutionJpaRepository.save(OptimizationMapper.toEntity(solution, objectMapper));
    }

    @Override
    @Transactional
    public void markFailed(UUID id, String error, OptimizationJobStatus finalStatus, Instant updatedAt) {
        jobJpaRepository.markFailed(id, error, finalStatus, OptimizationJobStatus.COMPLETED, updatedAt);
    }

    private void copyFields(OptimizationJob job, OptimizationJobEntity entity) {
        entity.setId(job.getId());
        entity.setScenarioId(job.getScenarioId());
        entity.setStatus(job.getStatus());
        entity.setProgress(job.getProgress());
        entity.setPhase(job.getPhase());
        entity.setErrorMessage(job.getErrorMessage());
        entity.setAttempts(job.getAttempts());
        entity.setCreatedAt(job.getCreatedAt());
        entity.setUpdatedAt(job.getUpdatedAt());
        entity.setCompletedAt(job.getCompletedAt());
    }
}
