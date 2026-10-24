package org.example.vros.service.interfaces;

import org.example.vros.model.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
    Customer save(Customer customer);
    List<Customer> findAll();
    Optional<Customer> findById(Long id);
    Optional<Customer> findByEmail(String email);
    List<Customer> searchByName(String keyword);
    Optional<Customer> findByIdWithDeliveries(Long id);
    int updateAddress(Long id, String address);
    void deleteById(Long id);
}
