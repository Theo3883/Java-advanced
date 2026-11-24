package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteStop(
        int sequence,
        Long deliveryId,
        double lat,
        double lng,
        int quantity,
        double legDistanceKm) {
}
