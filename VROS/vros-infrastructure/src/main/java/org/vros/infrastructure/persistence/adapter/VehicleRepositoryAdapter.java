package org.vros.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.vros.domain.model.Vehicle;
import org.vros.domain.port.VehicleRepository;
import org.vros.infrastructure.persistence.mapper.EntityMapper;
import org.vros.infrastructure.persistence.springdata.VehicleJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VehicleJpaRepository jpaRepo;

    @Override
    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public Vehicle save(Vehicle vehicle) {
        return EntityMapper.toDomain(jpaRepo.save(EntityMapper.toEntity(vehicle)));
    }

    @Override
    @Cacheable("vehicles")
    public List<Vehicle> findAll() {
        return EntityMapper.toVehicleDomainList(jpaRepo.findAll());
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return jpaRepo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public List<Vehicle> findByAvailableTrue() {
        return EntityMapper.toVehicleDomainList(jpaRepo.findByAvailableTrue());
    }

    @Override
    public List<Vehicle> findByDepotId(Long depotId) {
        return EntityMapper.toVehicleDomainList(jpaRepo.findByDepotId(depotId));
    }

    @Override
    public List<Vehicle> findAvailableWithMinCapacity(int minCapacity) {
        return EntityMapper.toVehicleDomainList(jpaRepo.findAvailableWithMinCapacity(minCapacity));
    }

    @Override
    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public int updateAvailability(Long id, boolean available) {
        return jpaRepo.updateAvailability(id, available);
    }

    @Override
    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
