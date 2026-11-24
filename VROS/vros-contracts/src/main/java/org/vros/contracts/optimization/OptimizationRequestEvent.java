package org.vros.contracts.optimization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published by VROS when a scenario must be optimized.
 *
 * <p>The payload contains a complete snapshot of the problem so the worker does not need
 * to call back into the VROS database.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizationRequestEvent(
        int schemaVersion,
        UUID jobId,
        Long scenarioId,
        String scenarioName,
        List<VehicleSpec> vehicles,
        List<DeliverySpec> deliveries,
        Instant requestedAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
}
