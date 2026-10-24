package org.example.vros.repository;

import org.example.vros.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    List<Customer> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.deliveries WHERE c.id = :id")
    Optional<Customer> findByIdWithDeliveries(Long id);

    // update address
    @Modifying
    @Transactional
    @Query("UPDATE Customer c SET c.address = :address WHERE c.id = :id")
    int updateAddress(Long id, String address);
}
