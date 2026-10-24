package org.example.vros.service.interfaces;

import org.example.vros.model.Driver;

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
