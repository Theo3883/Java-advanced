package org.vros.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.vros.domain.model.Driver;
import org.vros.domain.port.DriverRepository;
import org.vros.infrastructure.persistence.mapper.EntityMapper;
import org.vros.infrastructure.persistence.springdata.DriverJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverRepositoryAdapter implements DriverRepository {

    private final DriverJpaRepository jpaRepo;

    @Override
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public Driver save(Driver driver) {
        return EntityMapper.toDomain(jpaRepo.save(EntityMapper.toEntity(driver)));
    }

    @Override
    @Cacheable("drivers")
    public List<Driver> findAll() {
        return EntityMapper.toDriverDomainList(jpaRepo.findAll());
    }

    @Override
    @Cacheable(value = "drivers", key = "#id")
    public Optional<Driver> findById(Long id) {
        return jpaRepo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Driver> findByEmail(String email) {
        return jpaRepo.findByEmail(email).map(EntityMapper::toDomain);
    }

    @Override
    public List<Driver> findUnassignedDrivers() {
        return EntityMapper.toDriverDomainList(jpaRepo.findUnassignedDrivers());
    }

    @Override
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public int updatePhoneByEmail(String email, String phone) {
        return jpaRepo.updatePhoneByEmail(email, phone);
    }

    @Override
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
