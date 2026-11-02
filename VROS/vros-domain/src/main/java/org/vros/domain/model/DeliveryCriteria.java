package org.vros.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryCriteria {

    /** Filter by exact customer ID. */
    private Long customerId;

    /** Centre latitude for area-based search. Requires centerLng + radiusKm. */
    private Double centerLat;

    /** Centre longitude for area-based search. Requires centerLat + radiusKm. */
    private Double centerLng;

    /**
     * Radius in kilometres around (centerLat, centerLng).
     * Uses a bounding-box approximation (1° lat ~ 111 km).
     */
    private Double radiusKm;

    /** Include only deliveries whose quantity >= minQuantity. */
    private Integer minQuantity;

    /** Include only deliveries on this calendar date. */
    private LocalDate date;

    /** Include only deliveries on or after this date (inclusive). */
    private LocalDate dateFrom;

    /** Include only deliveries on or before this date (inclusive). */
    private LocalDate dateTo;
}
