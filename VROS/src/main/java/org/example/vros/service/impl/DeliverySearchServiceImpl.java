package org.example.vros.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.example.vros.dto.DeliveryFilterDto;
import org.example.vros.model.Customer;
import org.example.vros.model.Delivery;
import org.example.vros.model.embeddable.Coordinates;
import org.example.vros.service.interfaces.DeliverySearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic delivery search using the JPA Criteria API.
 * Each filter field is independently optional - only non-null fields
 * are added as predicates
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliverySearchServiceImpl implements DeliverySearchService {

    private static final double KM_PER_DEGREE_LAT = 111.0;

    private final EntityManager entityManager;

    @Override
    public List<Delivery> search(DeliveryFilterDto filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Delivery> query = cb.createQuery(Delivery.class);
        Root<Delivery> delivery = query.from(Delivery.class);

        // Eager join customer to allow filtering by customer properties & avoid N+1 queries
        Join<Delivery, Customer> customer = delivery.join("customer", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();

        // 1. Filter by specific customer
        if (filter.getCustomerId() != null) {
            predicates.add(cb.equal(customer.get("id"), filter.getCustomerId()));
        }

        // 2. Filter by geographic area (bounding box approximation)
        if (filter.getCenterLat() != null && filter.getCenterLng() != null && filter.getRadiusKm() != null) {
            double deltaLat = filter.getRadiusKm() / KM_PER_DEGREE_LAT;
            double cosLat = Math.cos(Math.toRadians(filter.getCenterLat()));
            double deltaLng = filter.getRadiusKm() / (KM_PER_DEGREE_LAT * (cosLat == 0 ? 1.0 : Math.abs(cosLat)));

            Path<Coordinates> coords = customer.get("coordinates");
            predicates.add(cb.between(coords.get("lat"),
                    filter.getCenterLat() - deltaLat,
                    filter.getCenterLat() + deltaLat));
            predicates.add(cb.between(coords.get("lng"),
                    filter.getCenterLng() - deltaLng,
                    filter.getCenterLng() + deltaLng));
        }

        // 3. Filter by demand exceeding threshold
        if (filter.getMinQuantity() != null) {
            predicates.add(cb.greaterThanOrEqualTo(delivery.get("quantity"), filter.getMinQuantity()));
        }

        // 4. Filter by specific date or date range
        if (filter.getDate() != null) {
            LocalDateTime startOfDay = filter.getDate().atStartOfDay();
            LocalDateTime endOfDay = filter.getDate().plusDays(1).atStartOfDay();
            predicates.add(cb.between(delivery.get("timestamp"), startOfDay, endOfDay));
        } else {
            if (filter.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        delivery.get("timestamp"),
                        filter.getDateFrom().atStartOfDay()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(cb.lessThan(
                        delivery.get("timestamp"),
                        filter.getDateTo().plusDays(1).atStartOfDay()));
            }
        }

        query.select(delivery)
             .where(predicates.toArray(new Predicate[0]))
             .orderBy(cb.desc(delivery.get("timestamp")));

        return entityManager.createQuery(query).getResultList();
    }
}
