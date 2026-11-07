package org.vros.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.vros.domain.model.Scenario;
import org.vros.domain.port.ScenarioRepository;
import org.vros.infrastructure.persistence.mapper.EntityMapper;
import org.vros.infrastructure.persistence.springdata.ScenarioJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioRepositoryAdapter implements ScenarioRepository {

    private final ScenarioJpaRepository jpaRepo;

    @Override
    @Transactional
    public Scenario save(Scenario scenario) {
        return EntityMapper.toDomain(jpaRepo.save(EntityMapper.toEntity(scenario)));
    }

    @Override
    public List<Scenario> findAll() {
        return EntityMapper.toScenarioDomainList(jpaRepo.findAll());
    }

    @Override
    public Optional<Scenario> findById(Long id) {
        return jpaRepo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Scenario> findByIdWithAssociations(Long id) {
        return jpaRepo.findByIdWithAssociations(id).map(EntityMapper::toDomain);
    }

    @Override
    public List<Scenario> findByNameContainingIgnoreCase(String keyword) {
        return EntityMapper.toScenarioDomainList(jpaRepo.findByNameContainingIgnoreCase(keyword));
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepo.existsById(id);
    }

    @Override
    public boolean existsByDeliveryIdAndDriverSubject(Long deliveryId, String keycloakSubject) {
        if (deliveryId == null || keycloakSubject == null) {
            return false;
        }
        return jpaRepo.existsByDeliveryIdAndDriverSubject(deliveryId, keycloakSubject);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
