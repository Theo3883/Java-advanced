package org.vros.application.service;

import org.vros.domain.model.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverService {
    Driver save(Driver driver);
    List<Driver> findAll();
    Optional<Driver> findById(Long id);
    Optional<Driver> findByEmail(String email);
    List<Driver> findUnassignedDrivers();
    int updatePhone(String email, String phone);
    void deleteById(Long id);
}
