package org.vros.domain.port;

import org.vros.domain.model.Delivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository {

    Delivery save(Delivery delivery);
    List<Delivery> findAll();
    Optional<Delivery> findById(Long id);
    List<Delivery> findByCustomerId(Long customerId);
    List<Delivery> findByTimestampAfter(LocalDateTime since);
    Integer sumQuantityByCustomerId(Long customerId);
    void deleteById(Long id);
}
