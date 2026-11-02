package org.vros.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class ScenarioRequest {

    @NotBlank(message = "{scenario.name.required}")
    @Size(max = 200, message = "{scenario.name.size}")
    private String name;

    private Set<Long> vehicleIds;

    private Set<Long> deliveryIds;
}
