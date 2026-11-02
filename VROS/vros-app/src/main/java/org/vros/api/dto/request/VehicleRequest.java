package org.vros.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VehicleRequest {

    @NotBlank(message = "{vehicle.name.required}")
    @Size(max = 100, message = "{vehicle.name.size}")
    private String name;

    @Min(value = 1, message = "{vehicle.capacity.min}")
    private int capacity;

    private boolean available = true;

    private Long depotId;

    private Long driverId;
}
