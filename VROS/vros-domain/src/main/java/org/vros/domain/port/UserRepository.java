package org.vros.domain.port;

import org.vros.domain.model.User;

import java.util.Optional;

/**
 * Pure domain port for the local Keycloak identity projection.
 * No Spring Security or Keycloak types may appear in this contract.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByKeycloakSubject(String subject);

    Optional<User> findByEmail(String email);

    void deleteById(Long id);
}
