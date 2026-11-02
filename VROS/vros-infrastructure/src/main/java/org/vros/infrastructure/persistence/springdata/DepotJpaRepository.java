package org.vros.infrastructure.persistence.springdata;

import org.vros.infrastructure.persistence.entity.DepotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotJpaRepository extends JpaRepository<DepotEntity, Long> {

    Optional<DepotEntity> findByName(String name);

    @Query("SELECT d FROM DepotEntity d LEFT JOIN FETCH d.vehicles WHERE d.id = :id")
    Optional<DepotEntity> findByIdWithVehicles(Long id);
}
