package org.vros.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "vehicle")
public class Driver extends Person {

    private Long id;
    private Vehicle vehicle;

    @Builder
    public Driver(String name, String email, String phone) {
        super(name, email, phone);
    }
}
