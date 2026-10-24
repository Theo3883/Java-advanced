package org.example.vros.service.interfaces;

import org.example.vros.model.Depot;

import java.util.List;
import java.util.Optional;

public interface DepotService {
    Depot save(Depot depot);
    List<Depot> findAll();
    Optional<Depot> findById(Long id);
    Optional<Depot> findByIdWithVehicles(Long id);
    Optional<Depot> findByName(String name);
    void deleteById(Long id);
}
