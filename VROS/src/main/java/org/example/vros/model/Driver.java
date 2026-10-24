package org.example.vros.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "vehicle")
public class Driver extends Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Inverse side of the Vehicle <-> Driver one-to-one. */
    @OneToOne(mappedBy = "driver")
    private Vehicle vehicle;

    @Builder
    public Driver(String name, String email, String phone) {
        super(name, email, phone);
    }
}
