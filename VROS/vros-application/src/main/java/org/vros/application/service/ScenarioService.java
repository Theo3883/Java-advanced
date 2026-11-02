package org.vros.application.service;

import org.vros.domain.model.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioService {
    Scenario save(Scenario scenario);
    List<Scenario> findAll();
    Optional<Scenario> findById(Long id);
    Optional<Scenario> findByIdWithAssociations(Long id);
    List<Scenario> searchByName(String keyword);
    Scenario update(Long id, Scenario updated);
    void deleteById(Long id);
}
