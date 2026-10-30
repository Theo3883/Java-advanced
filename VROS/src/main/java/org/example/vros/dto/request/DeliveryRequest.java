package org.example.vros.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryRequest {

    @NotNull(message = "{delivery.customerId.required}")
    private Long customerId;

    @Min(value = 1, message = "{delivery.quantity.min}")
    private int quantity;

    private String notes;

    private LocalDateTime timestamp;
}
