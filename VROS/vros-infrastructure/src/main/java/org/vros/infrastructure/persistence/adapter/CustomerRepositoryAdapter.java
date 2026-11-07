package org.vros.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.vros.domain.model.Customer;
import org.vros.domain.port.CustomerRepository;
import org.vros.infrastructure.persistence.entity.CustomerEntity;
import org.vros.infrastructure.persistence.mapper.EntityMapper;
import org.vros.infrastructure.persistence.springdata.CustomerJpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepo;

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public Customer save(Customer customer) {
        return EntityMapper.toDomain(jpaRepo.save(EntityMapper.toEntity(customer)));
    }

    @Override
    @Cacheable("customers")
    public List<Customer> findAll() {
        return EntityMapper.toCustomerDomainList(jpaRepo.findAll());
    }

    @Override
    @Cacheable(value = "customers", key = "#id")
    public Optional<Customer> findById(Long id) {
        return jpaRepo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaRepo.findByEmail(email).map(EntityMapper::toDomain);
    }

    @Override
    public List<Customer> findByNameContainingIgnoreCase(String keyword) {
        return EntityMapper.toCustomerDomainList(jpaRepo.findByNameContainingIgnoreCase(keyword));
    }

    @Override
    public Optional<Customer> findByIdWithDeliveries(Long id) {
        return jpaRepo.findByIdWithDeliveries(id).map(EntityMapper::toDomainWithDeliveries);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public int updateAddress(Long id, String address) {
        return jpaRepo.updateAddress(id, address);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }
}
