package org.example.vros.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.vros.model.embeddable.Coordinates;

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
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Embedded
    private Coordinates coordinates;

    /** A depot may accommodate any number of vehicles. */
    @OneToMany(mappedBy = "depot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();
}
