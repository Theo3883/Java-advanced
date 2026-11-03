package org.vros.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.vros.domain.model.Depot;
import org.vros.domain.port.DepotRepository;
import org.vros.infrastructure.persistence.mapper.EntityMapper;
import org.vros.infrastructure.persistence.springdata.DepotJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepotRepositoryAdapter implements DepotRepository {

    private final DepotJpaRepository jpaRepo;

    @Override
    @Transactional
    @CacheEvict(value = "depots", allEntries = true)
    public Depot save(Depot depot) {
        return EntityMapper.toDomain(jpaRepo.save(EntityMapper.toEntity(depot)));
    }

    @Override
    @Cacheable("depots")
    public List<Depot> findAll() {
        return EntityMapper.toDepotDomainList(jpaRepo.findAll());
    }

    @Override
    @Cacheable(value = "depots", key = "#id")
    public Optional<Depot> findById(Long id) {
        return jpaRepo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Depot> findByName(String name) {
        return jpaRepo.findByName(name).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Depot> findByIdWithVehicles(Long id) {
        return jpaRepo.findByIdWithVehicles(id).map(EntityMapper::toDomain);
    }

    @Override
    @Transactional
    @CacheEvict(value = "depots", allEntries = true)
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
