package org.vros.domain.port;

import org.vros.domain.model.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverRepository {

    Driver save(Driver driver);
    List<Driver> findAll();
    Optional<Driver> findById(Long id);
    Optional<Driver> findByEmail(String email);
    List<Driver> findUnassignedDrivers();
    int updatePhoneByEmail(String email, String phone);
    void deleteById(Long id);
}
