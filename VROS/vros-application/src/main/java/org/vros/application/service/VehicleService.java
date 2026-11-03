package org.vros.application.service;

import org.vros.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface VehicleService {
    Vehicle save(Vehicle vehicle);
    List<Vehicle> findAll();
    Optional<Vehicle> findById(Long id);
    List<Vehicle> findAvailable();
    List<Vehicle> findByDepot(Long depotId);
    List<Vehicle> findAvailableWithMinCapacity(int minCapacity);
    int setAvailability(Long id, boolean available);
    void deleteById(Long id);
}
