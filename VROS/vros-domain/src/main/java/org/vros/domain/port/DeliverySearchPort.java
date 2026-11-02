package org.vros.domain.port;

import org.vros.domain.model.Delivery;
import org.vros.domain.model.DeliveryCriteria;

import java.util.List;

public interface DeliverySearchPort {

    List<Delivery> search(DeliveryCriteria criteria);
}
