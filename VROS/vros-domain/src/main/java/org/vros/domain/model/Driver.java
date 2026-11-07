package org.vros.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"vehicle", "user"})
public class Driver extends Person {

    private Long id;
    private Vehicle vehicle;
    /** Keycloak-backed identity linked to this driver profile (may be null until first login). */
    private User user;

    @Builder
    public Driver(String name, String email, String phone) {
        super(name, email, phone);
    }
}
