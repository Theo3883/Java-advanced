package org.example.vros.repository;

import org.example.vros.model.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    List<Scenario> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT s FROM Scenario s LEFT JOIN FETCH s.vehicles LEFT JOIN FETCH s.deliveries WHERE s.id = :id")
    Optional<Scenario> findByIdWithAssociations(@Param("id") Long id);
}
