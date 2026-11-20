package org.vros.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemDelivery {

    private Long id;
    private Long customerId;
    private int quantity;
    private Coordinates coordinates;
}
