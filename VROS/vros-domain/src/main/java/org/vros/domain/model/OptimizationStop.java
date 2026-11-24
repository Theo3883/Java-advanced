package org.vros.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationStop {

    private int sequence;
    private Long deliveryId;
    private double lat;
    private double lng;
    private int quantity;
    private double legDistanceKm;
}
