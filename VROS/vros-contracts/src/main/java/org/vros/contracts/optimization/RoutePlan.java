package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoutePlan(
        Long vehicleId,
        String vehicleName,
        int load,
        double distanceKm,
        List<RouteStop> stops) {
}
