package org.example.vros.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.vros.model.Vehicle;
import org.example.vros.repository.VehicleRepository;
import org.example.vros.service.interfaces.VehicleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Cacheable("vehicles")
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    @Override
    @Cacheable(value = "vehicles", key = "#id")
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
    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public int setAvailability(Long id, boolean available) {
        return vehicleRepository.updateAvailability(id, available);
    }

    @Override
    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public void deleteById(Long id) {
        vehicleRepository.deleteById(id);
    }
}
