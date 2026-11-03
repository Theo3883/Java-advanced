package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.service.VehicleService;
import org.vros.domain.model.Vehicle;
import org.vros.domain.port.VehicleRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Override
    public List<Vehicle> findAvailable() {
        return vehicleRepository.findByAvailableTrue();
    }

    @Override
    public List<Vehicle> findByDepot(Long depotId) {
        return vehicleRepository.findByDepotId(depotId);
    }

    @Override
    public List<Vehicle> findAvailableWithMinCapacity(int minCapacity) {
        return vehicleRepository.findAvailableWithMinCapacity(minCapacity);
    }

    @Override
    public int setAvailability(Long id, boolean available) {
        return vehicleRepository.updateAvailability(id, available);
    }

    @Override
    public void deleteById(Long id) {
        vehicleRepository.deleteById(id);
    }
}
