package org.vros.infrastructure.persistence.springdata;

import org.vros.infrastructure.persistence.entity.DeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeliveryJpaRepository extends JpaRepository<DeliveryEntity, Long> {

    List<DeliveryEntity> findByCustomerId(Long customerId);

    List<DeliveryEntity> findByTimestampAfter(LocalDateTime since);

    @Query("SELECT SUM(d.quantity) FROM DeliveryEntity d WHERE d.customer.id = :customerId")
    Integer sumQuantityByCustomerId(Long customerId);
}
