package org.example.vros.repository;

import org.example.vros.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByEmail(String email);

    @Query("SELECT d FROM Driver d WHERE d.vehicle IS NULL")
    List<Driver> findUnassignedDrivers();

    //  update phone by email
    @Modifying
    @Transactional
    @Query("UPDATE Driver d SET d.phone = :phone WHERE d.email = :email")
    int updatePhoneByEmail(String email, String phone);
}
