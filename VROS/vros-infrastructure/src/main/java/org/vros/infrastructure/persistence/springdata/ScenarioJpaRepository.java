package org.vros.infrastructure.persistence.springdata;

import org.vros.infrastructure.persistence.entity.ScenarioEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioJpaRepository extends JpaRepository<ScenarioEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    List<ScenarioEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    Optional<ScenarioEntity> findById(Long id);

    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    List<ScenarioEntity> findByNameContainingIgnoreCase(String keyword);

    @EntityGraph(attributePaths = {"vehicles", "deliveries"})
    @Query("SELECT s FROM ScenarioEntity s WHERE s.id = :id")
    Optional<ScenarioEntity> findByIdWithAssociations(@Param("id") Long id);

    /**
     * True when the delivery is part of a scenario whose vehicles include one driven
     * by the driver identified by the given Keycloak subject.
     */
    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM ScenarioEntity s
            JOIN s.deliveries d
            JOIN s.vehicles v
            JOIN v.driver dr
            JOIN dr.user u
            WHERE d.id = :deliveryId AND u.keycloakSubject = :subject
            """)
    boolean existsByDeliveryIdAndDriverSubject(@Param("deliveryId") Long deliveryId,
                                               @Param("subject") String subject);
}
