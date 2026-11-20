package org.vros.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitOptimizationRequest {

    @NotNull(message = "{optimization.scenarioId.required}")
    private Long scenarioId;
}
