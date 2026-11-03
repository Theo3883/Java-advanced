package org.vros.domain.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "vehicles")
public class Depot {

    private Long id;
    private String name;
    private Coordinates coordinates;
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();
}
