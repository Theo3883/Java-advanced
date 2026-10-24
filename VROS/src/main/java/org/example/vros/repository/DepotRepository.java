package org.example.vros.repository;

import org.example.vros.model.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {

    Optional<Depot> findByName(String name);

    // fetch depot with its vehicles eagerly
    @Query("SELECT d FROM Depot d LEFT JOIN FETCH d.vehicles WHERE d.id = :id")
    Optional<Depot> findByIdWithVehicles(Long id);
}
