package org.vros.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OptimizationRouteResponse {
    private Long vehicleId;
    private String vehicleName;
    private int load;
    private double distanceKm;
    private List<OptimizationStopResponse> stops;
}
