package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.service.ScenarioService;
import org.vros.domain.exception.ResourceNotFoundException;
import org.vros.domain.model.Scenario;
import org.vros.domain.port.ScenarioRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioRepository repo;

    @Override
    public Scenario save(Scenario scenario) {
        return repo.save(scenario);
    }

    @Override
    public List<Scenario> findAll() {
        return repo.findAll();
    }

    @Override
    public Optional<Scenario> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public Optional<Scenario> findByIdWithAssociations(Long id) {
        return repo.findByIdWithAssociations(id);
    }

    @Override
    public List<Scenario> searchByName(String keyword) {
        return repo.findByNameContainingIgnoreCase(keyword);
    }

    @Override
    public Scenario update(Long id, Scenario updated) {
        Scenario existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", id));
        existing.setName(updated.getName());
        existing.setVehicles(updated.getVehicles());
        existing.setDeliveries(updated.getDeliveries());
        return repo.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Scenario", id);
        }
        repo.deleteById(id);
    }
}
