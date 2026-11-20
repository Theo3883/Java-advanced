package org.vros.worker.solver;

import org.springframework.stereotype.Component;
import org.vros.contracts.optimization.DeliverySpec;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.contracts.optimization.RoutePlan;
import org.vros.contracts.optimization.RouteStop;
import org.vros.contracts.optimization.VehicleSpec;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy nearest-neighbor capacitated VRP solver.
 *
 * <p>For every vehicle, starting at its depot, repeatedly pick the closest remaining delivery that
 * still fits in the vehicle. When the vehicle is full or no delivery fits, add the return leg to
 * the depot and continue with the next vehicle. Deliveries that no vehicle can serve are reported
 * as unassigned.</p>
 *
 * <p>Complexity is O(V × N²); acceptable for homework-sized scenarios. A spatial index or
 * OR-Tools would be the next step for production workloads.</p>
 */
@Component
public class NearestNeighborVrpSolver implements VrpSolver {

    public static final String ALGORITHM = "GREEDY_NEAREST_NEIGHBOR";

    @Override
    public OptimizationResultEvent solve(OptimizationRequestEvent request, ProgressListener progressListener) {
        if (request == null || request.jobId() == null) {
            throw new IllegalArgumentException("Optimization request and job id are required");
        }
        ProgressListener listener = progressListener == null ? ProgressListener.NOOP : progressListener;
        long startedAt = System.nanoTime();

        List<DeliverySpec> remaining = new ArrayList<>(
                request.deliveries() == null ? List.of() : request.deliveries());
        // Stable input order makes tie-breaking (and therefore the benchmark) reproducible.
        remaining.sort(Comparator.comparing(DeliverySpec::id,
                Comparator.nullsLast(Comparator.naturalOrder())));
        int totalDeliveries = remaining.size();
        int assignedDeliveries = 0;
        List<RoutePlan> routes = new ArrayList<>();

        for (VehicleSpec vehicle : usableVehicles(request.vehicles())) {
            if (remaining.isEmpty()) {
                break;
            }
            double currentLat = vehicle.depotLat();
            double currentLng = vehicle.depotLng();
            int load = 0;
            double routeDistanceKm = 0;
            List<RouteStop> stops = new ArrayList<>();

            while (true) {
                DeliverySpec candidate = nearestFeasible(
                        remaining, vehicle.capacity() - load, currentLat, currentLng);
                if (candidate == null) {
                    break;
                }
                double legKm = HaversineDistance.between(
                        currentLat, currentLng, candidate.lat(), candidate.lng());
                stops.add(new RouteStop(
                        stops.size() + 1,
                        candidate.id(),
                        candidate.lat(),
                        candidate.lng(),
                        candidate.quantity(),
                        round(legKm)));
                routeDistanceKm += legKm;
                load += candidate.quantity();
                currentLat = candidate.lat();
                currentLng = candidate.lng();
                remaining.remove(candidate);
                assignedDeliveries++;

                listener.onProgress(
                        progressOf(assignedDeliveries, totalDeliveries),
                        "ASSIGNING_DELIVERIES",
                        "Assigned " + assignedDeliveries + " of " + totalDeliveries + " deliveries");
            }

            if (!stops.isEmpty()) {
                routeDistanceKm += HaversineDistance.between(
                        currentLat, currentLng, vehicle.depotLat(), vehicle.depotLng());
                routes.add(new RoutePlan(
                        vehicle.id(),
                        vehicle.name(),
                        load,
                        round(routeDistanceKm),
                        List.copyOf(stops)));
            }
        }

        listener.onProgress(95, "FINALIZING", "Finalizing routes");

        double totalDistanceKm = routes.stream().mapToDouble(RoutePlan::distanceKm).sum();
        List<Long> unassignedDeliveryIds = remaining.stream().map(DeliverySpec::id).toList();
        long computationMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        return new OptimizationResultEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION,
                request.jobId(),
                request.scenarioId(),
                ALGORITHM,
                round(totalDistanceKm),
                computationMillis,
                unassignedDeliveryIds.isEmpty(),
                unassignedDeliveryIds,
                routes,
                Instant.now());
    }

    private List<VehicleSpec> usableVehicles(List<VehicleSpec> vehicles) {
        if (vehicles == null) {
            return List.of();
        }
        return vehicles.stream()
                .filter(vehicle -> vehicle != null)
                .filter(vehicle -> vehicle.capacity() > 0)
                .filter(vehicle -> isCoordinate(vehicle.depotLat(), vehicle.depotLng()))
                .sorted(Comparator.comparing(VehicleSpec::id,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private DeliverySpec nearestFeasible(
            List<DeliverySpec> remaining, int capacityLeft, double fromLat, double fromLng) {
        DeliverySpec best = null;
        double bestDistance = Double.MAX_VALUE;
        for (DeliverySpec delivery : remaining) {
            if (delivery.quantity() <= 0 || delivery.quantity() > capacityLeft) {
                continue;
            }
            double distance = HaversineDistance.between(fromLat, fromLng, delivery.lat(), delivery.lng());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = delivery;
            }
        }
        return best;
    }

    private boolean isCoordinate(double lat, double lng) {
        return !Double.isNaN(lat) && !Double.isNaN(lng)
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180;
    }

    private int progressOf(int assigned, int total) {
        if (total <= 0) {
            return 90;
        }
        return (int) Math.floor(assigned * 90.0 / total);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
