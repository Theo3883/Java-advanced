package org.example.vros.dto.mapper;

import org.example.vros.dto.response.CustomerResponse;
import org.example.vros.dto.response.DeliveryResponse;
import org.example.vros.dto.response.ScenarioResponse;
import org.example.vros.dto.response.VehicleResponse;
import org.example.vros.model.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure-function mapper (no state)
 */
public final class DtoMapper {

    private DtoMapper() {}

    public static CustomerResponse toResponse(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .lat(c.getCoordinates() != null ? c.getCoordinates().getLat() : null)
                .lng(c.getCoordinates() != null ? c.getCoordinates().getLng() : null)
                .build();
    }

    public static VehicleResponse toResponse(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .capacity(v.getCapacity())
                .available(v.isAvailable())
                .depotId(v.getDepot() != null ? v.getDepot().getId() : null)
                .depotName(v.getDepot() != null ? v.getDepot().getName() : null)
                .driverId(v.getDriver() != null ? v.getDriver().getId() : null)
                .driverName(v.getDriver() != null ? v.getDriver().getName() : null)
                .build();
    }

    public static DeliveryResponse toResponse(Delivery d) {
        return DeliveryResponse.builder()
                .id(d.getId())
                .customerId(d.getCustomer() != null ? d.getCustomer().getId() : null)
                .customerName(d.getCustomer() != null ? d.getCustomer().getName() : null)
                .quantity(d.getQuantity())
                .notes(d.getNotes())
                .timestamp(d.getTimestamp())
                .build();
    }

    public static ScenarioResponse toResponse(Scenario s) {
        Set<Long> vehicleIds = s.getVehicles().stream().map(Vehicle::getId).collect(Collectors.toSet());
        Set<Long> deliveryIds = s.getDeliveries().stream().map(Delivery::getId).collect(Collectors.toSet());
        return ScenarioResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .vehicleIds(vehicleIds)
                .deliveryIds(deliveryIds)
                .build();
    }
}
