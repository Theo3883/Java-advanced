package org.vros.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vros.infrastructure.persistence.entity.OptimizationSolutionEntity;

import java.util.UUID;

public interface OptimizationSolutionJpaRepository extends JpaRepository<OptimizationSolutionEntity, UUID> {
}
