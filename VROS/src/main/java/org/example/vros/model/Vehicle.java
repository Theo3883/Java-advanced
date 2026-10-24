package org.example.vros.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"depot", "driver"})
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    /** Each vehicle starts/ends at one depot; many vehicles per depot. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    /**
     * A vehicle may have at most one driver; a driver may be assigned to at most one vehicle.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;
}
