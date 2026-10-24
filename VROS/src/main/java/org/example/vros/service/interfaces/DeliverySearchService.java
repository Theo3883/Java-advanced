package org.example.vros.service.interfaces;

import org.example.vros.dto.DeliveryFilterDto;
import org.example.vros.model.Delivery;

import java.util.List;

/**
 * Criteria API-based dynamic search for deliveries.
 */
public interface DeliverySearchService {
    List<Delivery> search(DeliveryFilterDto filter);
}
