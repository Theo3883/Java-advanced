package org.vros.infrastructure.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.vros.domain.model.Delivery;
import org.vros.domain.model.DeliveryCriteria;
import org.vros.domain.port.DeliverySearchPort;
import org.vros.infrastructure.persistence.entity.CoordinatesEmbeddable;
import org.vros.infrastructure.persistence.entity.CustomerEntity;
import org.vros.infrastructure.persistence.entity.DeliveryEntity;
import org.vros.infrastructure.persistence.mapper.EntityMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic delivery search using the JPA Criteria API.
 * Each filter field is independently optional - only non-null fields
 * are added as predicates.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliverySearchAdapter implements DeliverySearchPort {

    private static final double KM_PER_DEGREE_LAT = 111.0;

    private final EntityManager entityManager;

    @Override
    public List<Delivery> search(DeliveryCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DeliveryEntity> query = cb.createQuery(DeliveryEntity.class);
        Root<DeliveryEntity> delivery = query.from(DeliveryEntity.class);

        // Eager join customer to allow filtering by customer properties & avoid N+1 queries
        Join<DeliveryEntity, CustomerEntity> customer = delivery.join("customer", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();

        // 1. Filter by specific customer
        if (criteria.getCustomerId() != null) {
            predicates.add(cb.equal(customer.get("id"), criteria.getCustomerId()));
        }

        // 2. Filter by geographic area (bounding box approximation)
        if (criteria.getCenterLat() != null && criteria.getCenterLng() != null && criteria.getRadiusKm() != null) {
            double deltaLat = criteria.getRadiusKm() / KM_PER_DEGREE_LAT;
            double cosLat = Math.cos(Math.toRadians(criteria.getCenterLat()));
            double deltaLng = criteria.getRadiusKm() / (KM_PER_DEGREE_LAT * (cosLat == 0 ? 1.0 : Math.abs(cosLat)));

            Path<CoordinatesEmbeddable> coords = customer.get("coordinates");
            predicates.add(cb.between(coords.get("lat"),
                    criteria.getCenterLat() - deltaLat,
                    criteria.getCenterLat() + deltaLat));
            predicates.add(cb.between(coords.get("lng"),
                    criteria.getCenterLng() - deltaLng,
                    criteria.getCenterLng() + deltaLng));
        }

        // 3. Filter by demand exceeding threshold
        if (criteria.getMinQuantity() != null) {
            predicates.add(cb.greaterThanOrEqualTo(delivery.get("quantity"), criteria.getMinQuantity()));
        }

        // 4. Filter by specific date or date range
        if (criteria.getDate() != null) {
            LocalDateTime startOfDay = criteria.getDate().atStartOfDay();
            LocalDateTime endOfDay = criteria.getDate().plusDays(1).atStartOfDay();
            predicates.add(cb.between(delivery.get("timestamp"), startOfDay, endOfDay));
        } else {
            if (criteria.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        delivery.get("timestamp"),
                        criteria.getDateFrom().atStartOfDay()));
            }
            if (criteria.getDateTo() != null) {
                predicates.add(cb.lessThan(
                        delivery.get("timestamp"),
                        criteria.getDateTo().plusDays(1).atStartOfDay()));
            }
        }

        query.select(delivery)
             .where(predicates.toArray(new Predicate[0]))
             .orderBy(cb.desc(delivery.get("timestamp")));

        return EntityMapper.toDeliveryDomainList(entityManager.createQuery(query).getResultList());
    }
}
