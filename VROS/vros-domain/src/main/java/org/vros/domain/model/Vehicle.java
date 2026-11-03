package org.vros.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"depot", "driver"})
public class Vehicle {

    private Long id;
    private String name;
    private int capacity;
    @Builder.Default
    private boolean available = true;
    private Depot depot;
    private Driver driver;
}
