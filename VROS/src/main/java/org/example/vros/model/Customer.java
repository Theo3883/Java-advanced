package org.example.vros.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.vros.model.embeddable.Coordinates;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "deliveries")
public class Customer extends Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String address;

    @Embedded
    private Coordinates coordinates;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Delivery> deliveries = new ArrayList<>();

    @Builder
    public Customer(String name, String email, String phone, String address, Coordinates coordinates) {
        super(name, email, phone);
        this.address = address;
        this.coordinates = coordinates;
        this.deliveries = new ArrayList<>();
    }
}
