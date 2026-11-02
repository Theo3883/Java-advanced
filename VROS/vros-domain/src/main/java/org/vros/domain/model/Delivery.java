package org.vros.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "customer")
public class Delivery {

    private Long id;
    private Customer customer;
    private int quantity;
    private String notes;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
