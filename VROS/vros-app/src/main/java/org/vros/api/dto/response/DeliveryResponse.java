package org.vros.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private int quantity;
    private String notes;
    private LocalDateTime timestamp;
}
