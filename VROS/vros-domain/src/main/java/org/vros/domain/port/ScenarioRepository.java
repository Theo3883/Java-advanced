package org.vros.domain.port;

import org.vros.domain.model.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository {

    Scenario save(Scenario scenario);
    List<Scenario> findAll();
    Optional<Scenario> findById(Long id);
    Optional<Scenario> findByIdWithAssociations(Long id);
    List<Scenario> findByNameContainingIgnoreCase(String keyword);
    boolean existsById(Long id);

    /**
     * Scenario-derived ownership fallback: true when the delivery belongs to a scenario
     * containing a vehicle whose driver is the Keycloak subject.
     */
    boolean existsByDeliveryIdAndDriverSubject(Long deliveryId, String keycloakSubject);

    void deleteById(Long id);
}
