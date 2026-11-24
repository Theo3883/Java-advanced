package org.vros.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemVehicle {

    private Long id;
    private String name;
    private int capacity;
    private boolean available;
    private Coordinates depot;
}
