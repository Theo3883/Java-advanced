package org.vros.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"customer", "assignedDriver"})
public class Delivery {

    private Long id;
    private Customer customer;
    /** Driver explicitly assigned to this delivery (ownership anchor for DRIVER access). */
    private Driver assignedDriver;
    private int quantity;
    private String notes;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
