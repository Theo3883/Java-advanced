package org.vros.application.service;

import org.vros.domain.model.Delivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryService {
    Delivery save(Delivery delivery);
    List<Delivery> findAll();
    Optional<Delivery> findById(Long id);
    List<Delivery> findByCustomer(Long customerId);
    List<Delivery> findSince(LocalDateTime since);
    Integer totalQuantityForCustomer(Long customerId);
    void deleteById(Long id);
}
