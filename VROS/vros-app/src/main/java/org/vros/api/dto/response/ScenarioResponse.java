package org.vros.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ScenarioResponse {
    private Long id;
    private String name;
    private Set<Long> vehicleIds;
    private Set<Long> deliveryIds;
}
