package org.example.vros.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.vros.model.Depot;
import org.example.vros.repository.DepotRepository;
import org.example.vros.service.interfaces.DepotService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepotServiceImpl implements DepotService {

    private final DepotRepository depotRepository;

    @Override
    @Transactional
    @CacheEvict(value = "depots", allEntries = true)
    public Depot save(Depot depot) {
        return depotRepository.save(depot);
    }

    @Override
    @Cacheable("depots")
    public List<Depot> findAll() {
        return depotRepository.findAll();
    }

    @Override
    @Cacheable(value = "depots", key = "#id")
    public Optional<Depot> findById(Long id) {
        return depotRepository.findById(id);
    }

    @Override
    public Optional<Depot> findByIdWithVehicles(Long id) {
        return depotRepository.findByIdWithVehicles(id);
    }

    @Override
    public Optional<Depot> findByName(String name) {
        return depotRepository.findByName(name);
    }

    @Override
    @Transactional
    @CacheEvict(value = "depots", allEntries = true)
    public void deleteById(Long id) {
        depotRepository.deleteById(id);
    }
}
