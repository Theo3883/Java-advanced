package org.vros.domain.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationRoute {

    private Long vehicleId;
    private String vehicleName;
    private int load;
    private double distanceKm;
    @Builder.Default
    private List<OptimizationStop> stops = new ArrayList<>();
}
