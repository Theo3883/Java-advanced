package org.vros.domain.port;

import org.vros.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);
    List<Vehicle> findAll();
    Optional<Vehicle> findById(Long id);
    List<Vehicle> findByAvailableTrue();
    List<Vehicle> findByDepotId(Long depotId);
    List<Vehicle> findAvailableWithMinCapacity(int minCapacity);
    int updateAvailability(Long id, boolean available);
    void deleteById(Long id);
}
