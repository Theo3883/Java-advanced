package org.vros.application.service;

import org.vros.domain.model.DeliveryCriteria;
import org.vros.domain.model.Delivery;

import java.util.List;

/**
 * Criteria API-based dynamic search for deliveries.
 */
public interface DeliverySearchService {
    List<Delivery> search(DeliveryCriteria filter);
}
