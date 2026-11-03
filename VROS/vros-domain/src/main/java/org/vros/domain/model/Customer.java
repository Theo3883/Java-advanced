package org.vros.domain.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "deliveries")
public class Customer extends Person {

    private Long id;
    private String address;
    private Coordinates coordinates;
    private List<Delivery> deliveries = new ArrayList<>();

    @Builder
    public Customer(String name, String email, String phone, String address, Coordinates coordinates) {
        super(name, email, phone);
        this.address = address;
        this.coordinates = coordinates;
        this.deliveries = new ArrayList<>();
    }
}
