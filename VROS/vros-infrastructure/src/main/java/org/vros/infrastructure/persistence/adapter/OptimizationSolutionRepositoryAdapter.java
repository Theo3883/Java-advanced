package org.vros.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vros.domain.model.OptimizationSolution;
import org.vros.domain.port.OptimizationSolutionRepository;
import org.vros.infrastructure.persistence.mapper.OptimizationMapper;
import org.vros.infrastructure.persistence.springdata.OptimizationSolutionJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptimizationSolutionRepositoryAdapter implements OptimizationSolutionRepository {

    private final OptimizationSolutionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<OptimizationSolution> findByJobId(UUID jobId) {
        return jpaRepository.findById(jobId)
                .map(entity -> OptimizationMapper.toDomain(entity, objectMapper));
    }
}
