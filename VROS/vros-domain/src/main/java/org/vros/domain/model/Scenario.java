package org.vros.domain.model;

import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"vehicles", "deliveries"})
public class Scenario {

    private Long id;
    private String name;
    @Builder.Default
    private Set<Vehicle> vehicles = new HashSet<>();
    @Builder.Default
    private Set<Delivery> deliveries = new HashSet<>();
}
