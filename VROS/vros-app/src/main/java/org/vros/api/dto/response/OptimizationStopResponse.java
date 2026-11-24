package org.vros.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptimizationStopResponse {
    private int sequence;
    private Long deliveryId;
    private double lat;
    private double lng;
    private int quantity;
    private double legDistanceKm;
}
