package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleSpec(
        Long id,
        String name,
        int capacity,
        double depotLat,
        double depotLng) {
}
