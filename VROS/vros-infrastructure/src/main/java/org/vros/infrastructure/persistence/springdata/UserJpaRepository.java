package org.vros.infrastructure.persistence.springdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vros.infrastructure.persistence.entity.UserEntity;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByKeycloakSubject(String keycloakSubject);

    Optional<UserEntity> findByEmail(String email);
}
