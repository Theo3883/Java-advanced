package org.vros.application.service;

import org.vros.domain.model.Depot;

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
