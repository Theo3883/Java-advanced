package org.example.vros.repository;

import org.example.vros.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByCustomerId(Long customerId);

    List<Delivery> findByTimestampAfter(LocalDateTime since);

    // total quantity for a customer
    @Query("SELECT SUM(d.quantity) FROM Delivery d WHERE d.customer.id = :customerId")
    Integer sumQuantityByCustomerId(Long customerId);
}
