package org.vros.domain.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationProblem {

    private Long scenarioId;
    private String scenarioName;
    @Builder.Default
    private List<ProblemVehicle> vehicles = new ArrayList<>();
    @Builder.Default
    private List<ProblemDelivery> deliveries = new ArrayList<>();
}
