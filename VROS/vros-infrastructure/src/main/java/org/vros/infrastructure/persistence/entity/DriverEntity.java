package org.vros.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"vehicle", "user"})
public class DriverEntity extends PersonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "driver")
    private VehicleEntity vehicle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

    @Builder
    public DriverEntity(String name, String email, String phone) {
        super(name, email, phone);
    }
}
