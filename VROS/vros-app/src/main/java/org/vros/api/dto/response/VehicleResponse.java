package org.vros.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleResponse {
    private Long id;
    private String name;
    private int capacity;
    private boolean available;
    private Long depotId;
    private String depotName;
    private Long driverId;
    private String driverName;
}
