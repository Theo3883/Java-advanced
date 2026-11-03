package org.vros.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Abstract base for entities that represent a person with contact info.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class Person {

    private String name;
    private String email;
    private String phone;

    protected Person(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
