package org.vros.application.impl;

import lombok.RequiredArgsConstructor;
import org.vros.application.service.DeliverySearchService;
import org.vros.domain.model.Delivery;
import org.vros.domain.model.DeliveryCriteria;
import org.vros.domain.port.DeliverySearchPort;

import java.util.List;

@RequiredArgsConstructor
public class DeliverySearchServiceImpl implements DeliverySearchService {

    private final DeliverySearchPort deliverySearchPort;

    @Override
    public List<Delivery> search(DeliveryCriteria criteria) {
        return deliverySearchPort.search(criteria);
    }
}
