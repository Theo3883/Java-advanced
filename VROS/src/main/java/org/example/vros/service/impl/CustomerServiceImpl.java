package org.example.vros.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.vros.model.Customer;
import org.example.vros.repository.CustomerRepository;
import org.example.vros.service.interfaces.CustomerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    @Cacheable("customers")
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    @Cacheable(value = "customers", key = "#id")
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    @Override
    public List<Customer> searchByName(String keyword) {
        return customerRepository.findByNameContainingIgnoreCase(keyword);
    }

    @Override
    public Optional<Customer> findByIdWithDeliveries(Long id) {
        return customerRepository.findByIdWithDeliveries(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public int updateAddress(Long id, String address) {
        return customerRepository.updateAddress(id, address);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }
}
