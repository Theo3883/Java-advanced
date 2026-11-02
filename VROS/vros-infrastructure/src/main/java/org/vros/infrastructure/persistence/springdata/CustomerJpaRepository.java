package org.vros.infrastructure.persistence.springdata;

import org.vros.infrastructure.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByEmail(String email);

    List<CustomerEntity> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT c FROM CustomerEntity c LEFT JOIN FETCH c.deliveries WHERE c.id = :id")
    Optional<CustomerEntity> findByIdWithDeliveries(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE CustomerEntity c SET c.address = :address WHERE c.id = :id")
    int updateAddress(Long id, String address);
}
