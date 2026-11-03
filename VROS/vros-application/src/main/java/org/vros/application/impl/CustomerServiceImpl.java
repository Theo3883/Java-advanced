package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.service.CustomerService;
import org.vros.domain.model.Customer;
import org.vros.domain.port.CustomerRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
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
    public int updateAddress(Long id, String address) {
        return customerRepository.updateAddress(id, address);
    }

    @Override
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }
}
