package org.example.vros.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstract base for entities that represent a person with contact info.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class Person {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    protected Person(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
