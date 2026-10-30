package org.example.vros.repository;

import org.example.vros.model.Scenario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    @Override
    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    List<Scenario> findAll();

    @Override
    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    Optional<Scenario> findById(Long id);

    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    List<Scenario> findByNameContainingIgnoreCase(String keyword);

    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    @Query("SELECT s FROM Scenario s WHERE s.id = :id")
    Optional<Scenario> findByIdWithAssociations(@Param("id") Long id);
}
