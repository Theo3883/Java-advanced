package org.vros.worker.solver;

import org.junit.jupiter.api.Test;
import org.vros.contracts.optimization.DeliverySpec;
import org.vros.contracts.optimization.OptimizationRequestEvent;
import org.vros.contracts.optimization.OptimizationResultEvent;
import org.vros.contracts.optimization.RoutePlan;
import org.vros.contracts.optimization.VehicleSpec;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NearestNeighborVrpSolverTest {

    private final NearestNeighborVrpSolver solver = new NearestNeighborVrpSolver();

    @Test
    void visitsDeliveriesInNearestNeighborOrderAndReturnsToDepot() {
        VehicleSpec van = vehicle(1L, "Van", 10, 0, 0);
        // Inserted out of order on purpose: the solver must pick the closest one first.
        List<DeliverySpec> deliveries = List.of(
                delivery(20L, 2, 0, 0.02),
                delivery(10L, 3, 0, 0.01));

        OptimizationResultEvent result = solver.solve(request(List.of(van), deliveries),
                VrpSolver.ProgressListener.NOOP);

        assertThat(result.feasible()).isTrue();
        assertThat(result.unassignedDeliveryIds()).isEmpty();
        assertThat(result.routes()).hasSize(1);

        RoutePlan route = result.routes().get(0);
        assertThat(route.vehicleId()).isEqualTo(1L);
        assertThat(route.load()).isEqualTo(5);
        assertThat(route.stops()).extracting(stop -> stop.deliveryId())
                .containsExactly(10L, 20L);
        assertThat(route.stops()).extracting(stop -> stop.sequence())
                .containsExactly(1, 2);
        // depot -> 0.01 -> 0.02 -> depot = 4 legs of ~1.111 km
        assertThat(route.distanceKm()).isCloseTo(4.448, org.assertj.core.data.Offset.offset(0.05));
        assertThat(result.totalDistanceKm()).isEqualTo(route.distanceKm());
        assertThat(result.algorithm()).isEqualTo(NearestNeighborVrpSolver.ALGORITHM);
    }

    @Test
    void respectsVehicleCapacity() {
        VehicleSpec small = vehicle(1L, "Small", 3, 0, 0);
        List<DeliverySpec> deliveries = List.of(
                delivery(1L, 2, 0, 0.01),
                delivery(2L, 2, 0, 0.02),
                delivery(3L, 2, 0, 0.03));

        OptimizationResultEvent result = solver.solve(request(List.of(small), deliveries),
                VrpSolver.ProgressListener.NOOP);

        assertThat(result.feasible()).isFalse();
        assertThat(result.routes()).hasSize(1);
        assertThat(result.routes().get(0).load()).isEqualTo(2);
        assertThat(result.unassignedDeliveryIds()).containsExactly(2L, 3L);
    }

    @Test
    void reportsDeliveriesLargerThanAnyVehicleAsUnassigned() {
        VehicleSpec van = vehicle(1L, "Van", 10, 0, 0);
        List<DeliverySpec> deliveries = List.of(delivery(1L, 50, 0, 0.01));

        OptimizationResultEvent result = solver.solve(request(List.of(van), deliveries),
                VrpSolver.ProgressListener.NOOP);

        assertThat(result.feasible()).isFalse();
        assertThat(result.routes()).isEmpty();
        assertThat(result.unassignedDeliveryIds()).containsExactly(1L);
    }

    @Test
    void usesSeveralVehiclesWhenOneIsNotEnough() {
        VehicleSpec first = vehicle(1L, "First", 2, 0, 0);
        VehicleSpec second = vehicle(2L, "Second", 2, 0, 0.5);
        List<DeliverySpec> deliveries = List.of(
                delivery(1L, 2, 0, 0.01),
                delivery(2L, 2, 0, 0.02));

        OptimizationResultEvent result = solver.solve(request(List.of(first, second), deliveries),
                VrpSolver.ProgressListener.NOOP);

        assertThat(result.feasible()).isTrue();
        assertThat(result.routes()).hasSize(2);
        assertThat(result.routes()).extracting(RoutePlan::vehicleId).containsExactly(1L, 2L);
    }

    @Test
    void handlesEmptyDeliveries() {
        VehicleSpec van = vehicle(1L, "Van", 10, 0, 0);

        OptimizationResultEvent result = solver.solve(request(List.of(van), List.of()),
                VrpSolver.ProgressListener.NOOP);

        assertThat(result.feasible()).isTrue();
        assertThat(result.routes()).isEmpty();
        assertThat(result.totalDistanceKm()).isZero();
    }

    @Test
    void reportsProgressWhileAssigning() {
        VehicleSpec van = vehicle(1L, "Van", 10, 0, 0);
        List<DeliverySpec> deliveries = List.of(
                delivery(1L, 1, 0, 0.01),
                delivery(2L, 1, 0, 0.02));
        List<Integer> progress = new ArrayList<>();

        solver.solve(request(List.of(van), deliveries),
                (value, phase, message) -> progress.add(value));

        assertThat(progress).isNotEmpty();
        assertThat(progress).contains(90, 95);
    }

    @Test
    void rejectsRequestsWithoutJobId() {
        OptimizationRequestEvent request = new OptimizationRequestEvent(
                1, null, 1L, "scenario", List.of(), List.of(), Instant.now());

        assertThatThrownBy(() -> solver.solve(request, VrpSolver.ProgressListener.NOOP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private OptimizationRequestEvent request(List<VehicleSpec> vehicles, List<DeliverySpec> deliveries) {
        return new OptimizationRequestEvent(
                OptimizationRequestEvent.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID(),
                1L,
                "scenario",
                vehicles,
                deliveries,
                Instant.now());
    }

    private VehicleSpec vehicle(Long id, String name, int capacity, double lat, double lng) {
        return new VehicleSpec(id, name, capacity, lat, lng);
    }

    private DeliverySpec delivery(Long id, int quantity, double lat, double lng) {
        return new DeliverySpec(id, id + 100, quantity, lat, lng);
    }
}
