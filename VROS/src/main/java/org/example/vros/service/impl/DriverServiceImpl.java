package org.example.vros.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.vros.model.Driver;
import org.example.vros.repository.DriverRepository;
import org.example.vros.service.interfaces.DriverService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Override
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public Driver save(Driver driver) {
        return driverRepository.save(driver);
    }

    @Override
    @Cacheable("drivers")
    public List<Driver> findAll() {
        return driverRepository.findAll();
    }

    @Override
    @Cacheable(value = "drivers", key = "#id")
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
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public int updatePhone(String email, String phone) {
        return driverRepository.updatePhoneByEmail(email, phone);
    }

    @Override
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public void deleteById(Long id) {
        driverRepository.deleteById(id);
    }
}
