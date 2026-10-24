package org.example.vros.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.vros.model.Delivery;
import org.example.vros.repository.DeliveryRepository;
import org.example.vros.service.interfaces.DeliveryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Override
    @Transactional
    @CacheEvict(value = "deliveries", allEntries = true)
    public Delivery save(Delivery delivery) {
        return deliveryRepository.save(delivery);
    }

    @Override
    @Cacheable("deliveries")
    public List<Delivery> findAll() {
        return deliveryRepository.findAll();
    }

    @Override
    @Cacheable(value = "deliveries", key = "#id")
    public Optional<Delivery> findById(Long id) {
        return deliveryRepository.findById(id);
    }

    @Override
    public List<Delivery> findByCustomer(Long customerId) {
        return deliveryRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Delivery> findSince(LocalDateTime since) {
        return deliveryRepository.findByTimestampAfter(since);
    }

    @Override
    public Integer totalQuantityForCustomer(Long customerId) {
        return deliveryRepository.sumQuantityByCustomerId(customerId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "deliveries", allEntries = true)
    public void deleteById(Long id) {
        deliveryRepository.deleteById(id);
    }
}
