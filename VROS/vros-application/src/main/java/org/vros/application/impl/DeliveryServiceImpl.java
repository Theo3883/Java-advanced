package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.service.DeliveryService;
import org.vros.domain.model.Delivery;
import org.vros.domain.port.DeliveryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Override
    public Delivery save(Delivery delivery) {
        return deliveryRepository.save(delivery);
    }

    @Override
    public List<Delivery> findAll() {
        return deliveryRepository.findAll();
    }

    @Override
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
    public void deleteById(Long id) {
        deliveryRepository.deleteById(id);
    }
}
