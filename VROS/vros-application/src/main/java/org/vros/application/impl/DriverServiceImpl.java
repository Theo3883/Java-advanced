package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.service.DriverService;
import org.vros.domain.model.Driver;
import org.vros.domain.port.DriverRepository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Override
    public Driver save(Driver driver) {
        return driverRepository.save(driver);
    }

    @Override
    public List<Driver> findAll() {
        return driverRepository.findAll();
    }

    @Override
    public Optional<Driver> findById(Long id) {
        return driverRepository.findById(id);
    }

    @Override
    public Optional<Driver> findByEmail(String email) {
        return driverRepository.findByEmail(email);
    }

    @Override
    public List<Driver> findUnassignedDrivers() {
        return driverRepository.findUnassignedDrivers();
    }

    @Override
    public int updatePhone(String email, String phone) {
        return driverRepository.updatePhoneByEmail(email, phone);
    }

    @Override
    public void deleteById(Long id) {
        driverRepository.deleteById(id);
    }
}
