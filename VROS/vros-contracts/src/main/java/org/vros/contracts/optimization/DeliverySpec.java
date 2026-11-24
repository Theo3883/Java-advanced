package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliverySpec(
        Long id,
        Long customerId,
        int quantity,
        double lat,
        double lng) {
}
