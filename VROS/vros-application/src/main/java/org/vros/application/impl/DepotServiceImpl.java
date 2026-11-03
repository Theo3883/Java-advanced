package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.service.DepotService;
import org.vros.domain.model.Depot;
import org.vros.domain.port.DepotRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DepotServiceImpl implements DepotService {

    private final DepotRepository depotRepository;

    @Override
    public Depot save(Depot depot) {
        return depotRepository.save(depot);
    }

    @Override
    public List<Depot> findAll() {
        return depotRepository.findAll();
    }

    @Override
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
    public void deleteById(Long id) {
        depotRepository.deleteById(id);
    }
}
