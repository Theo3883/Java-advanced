package org.vros.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vros.domain.model.OptimizationJobStatus;
import org.vros.infrastructure.persistence.entity.OptimizationJobEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OptimizationJobJpaRepository extends JpaRepository<OptimizationJobEntity, UUID> {

    List<OptimizationJobEntity> findByScenarioId(Long scenarioId);

    List<OptimizationJobEntity> findByStatus(OptimizationJobStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OptimizationJobEntity j
               set j.progress = :progress,
                   j.phase = :phase,
                   j.status = :running,
                   j.updatedAt = :updatedAt,
                   j.version = j.version + 1
             where j.id = :id
               and j.status <> :completed
               and j.status <> :failed
               and j.status <> :submissionFailed
            """)
    int updateProgress(@Param("id") UUID id,
                       @Param("progress") int progress,
                       @Param("phase") String phase,
                       @Param("running") OptimizationJobStatus running,
                       @Param("updatedAt") Instant updatedAt,
                       @Param("completed") OptimizationJobStatus completed,
                       @Param("failed") OptimizationJobStatus failed,
                       @Param("submissionFailed") OptimizationJobStatus submissionFailed);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OptimizationJobEntity j
               set j.status = :finalStatus,
                   j.errorMessage = :error,
                   j.updatedAt = :updatedAt,
                   j.completedAt = :updatedAt,
                   j.version = j.version + 1
             where j.id = :id
               and j.status <> :completed
            """)
    int markFailed(@Param("id") UUID id,
                   @Param("error") String error,
                   @Param("finalStatus") OptimizationJobStatus finalStatus,
                   @Param("completed") OptimizationJobStatus completed,
                   @Param("updatedAt") Instant updatedAt);
}
