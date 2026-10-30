package org.example.vros.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.vros.exception.ResourceNotFoundException;
import org.example.vros.model.Scenario;
import org.example.vros.repository.ScenarioRepository;
import org.example.vros.service.interfaces.ScenarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScenarioServiceImpl implements ScenarioService {

    private final ScenarioRepository repo;

    @Override
    @Transactional
    public Scenario save(Scenario scenario) {
        return repo.save(scenario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scenario> findAll() {
        return repo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Scenario> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Scenario> findByIdWithAssociations(Long id) {
        return repo.findByIdWithAssociations(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scenario> searchByName(String keyword) {
        return repo.findByNameContainingIgnoreCase(keyword);
    }

    @Override
    @Transactional
    public Scenario update(Long id, Scenario updated) {
        Scenario existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scenario", id));
        existing.setName(updated.getName());
        existing.setVehicles(updated.getVehicles());
        existing.setDeliveries(updated.getDeliveries());
        return repo.save(existing);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Scenario", id);
        }
        repo.deleteById(id);
    }
}
