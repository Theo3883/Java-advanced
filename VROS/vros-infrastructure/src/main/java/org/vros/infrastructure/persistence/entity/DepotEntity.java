package org.vros.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "depots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "vehicles")
public class DepotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Embedded
    private CoordinatesEmbeddable coordinates;

    @OneToMany(mappedBy = "depot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VehicleEntity> vehicles = new ArrayList<>();
}
