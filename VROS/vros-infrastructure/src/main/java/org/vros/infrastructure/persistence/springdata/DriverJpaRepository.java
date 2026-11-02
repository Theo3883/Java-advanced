package org.vros.infrastructure.persistence.springdata;

import org.vros.infrastructure.persistence.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverJpaRepository extends JpaRepository<DriverEntity, Long> {

    Optional<DriverEntity> findByEmail(String email);

    @Query("SELECT d FROM DriverEntity d WHERE d.vehicle IS NULL")
    List<DriverEntity> findUnassignedDrivers();

    @Modifying
    @Transactional
    @Query("UPDATE DriverEntity d SET d.phone = :phone WHERE d.email = :email")
    int updatePhoneByEmail(String email, String phone);
}
