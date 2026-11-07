package org.vros.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"deliveries", "user"})
public class CustomerEntity extends PersonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String address;

    @Embedded
    private CoordinatesEmbeddable coordinates;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DeliveryEntity> deliveries = new ArrayList<>();

    @Builder
    public CustomerEntity(String name, String email, String phone, String address, CoordinatesEmbeddable coordinates) {
        super(name, email, phone);
        this.address = address;
        this.coordinates = coordinates;
        this.deliveries = new ArrayList<>();
    }
}
