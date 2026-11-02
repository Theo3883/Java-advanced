package org.vros.domain.port;

import org.vros.domain.model.Depot;

import java.util.List;
import java.util.Optional;

public interface DepotRepository {

    Depot save(Depot depot);
    List<Depot> findAll();
    Optional<Depot> findById(Long id);
    Optional<Depot> findByName(String name);
    Optional<Depot> findByIdWithVehicles(Long id);
    void deleteById(Long id);
}
