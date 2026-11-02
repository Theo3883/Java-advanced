package org.vros.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "vehicle")
public class DriverEntity extends PersonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "driver")
    private VehicleEntity vehicle;

    @Builder
    public DriverEntity(String name, String email, String phone) {
        super(name, email, phone);
    }
}
