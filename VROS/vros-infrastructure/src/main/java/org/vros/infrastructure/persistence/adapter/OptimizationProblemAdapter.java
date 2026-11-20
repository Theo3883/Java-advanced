package org.vros.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vros.domain.model.Coordinates;
import org.vros.domain.model.OptimizationProblem;
import org.vros.domain.model.ProblemDelivery;
import org.vros.domain.model.ProblemVehicle;
import org.vros.domain.port.OptimizationProblemRepository;
import org.vros.infrastructure.persistence.entity.CustomerEntity;
import org.vros.infrastructure.persistence.entity.DeliveryEntity;
import org.vros.infrastructure.persistence.entity.ScenarioEntity;
import org.vros.infrastructure.persistence.entity.VehicleEntity;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptimizationProblemAdapter implements OptimizationProblemRepository {

    private final EntityManager entityManager;

    @Override
    public Optional<OptimizationProblem> findByScenarioId(Long scenarioId) {
        List<ScenarioEntity> scenarios = entityManager.createQuery("""
                        select distinct s from ScenarioEntity s
                        left join fetch s.vehicles v
                        left join fetch v.depot
                        where s.id = :id
                        """, ScenarioEntity.class)
                .setParameter("id", scenarioId)
                .getResultList();

        if (scenarios.isEmpty()) {
            return Optional.empty();
        }
        ScenarioEntity scenario = scenarios.get(0);

        List<DeliveryEntity> deliveries = entityManager.createQuery("""
                        select distinct d from ScenarioEntity s
                        join s.deliveries d
                        left join fetch d.customer c
                        where s.id = :id
                        """, DeliveryEntity.class)
                .setParameter("id", scenarioId)
                .getResultList();

        List<ProblemVehicle> vehicles = scenario.getVehicles().stream()
                .map(this::toProblemVehicle)
                .toList();
        List<ProblemDelivery> problemDeliveries = deliveries.stream()
                .map(this::toProblemDelivery)
                .toList();

        return Optional.of(OptimizationProblem.builder()
                .scenarioId(scenario.getId())
                .scenarioName(scenario.getName())
                .vehicles(vehicles)
                .deliveries(problemDeliveries)
                .build());
    }

    private ProblemVehicle toProblemVehicle(VehicleEntity vehicle) {
        Coordinates depot = vehicle.getDepot() == null || vehicle.getDepot().getCoordinates() == null
                ? null
                : new Coordinates(
                        vehicle.getDepot().getCoordinates().getLat(),
                        vehicle.getDepot().getCoordinates().getLng());
        return ProblemVehicle.builder()
                .id(vehicle.getId())
                .name(vehicle.getName())
                .capacity(vehicle.getCapacity())
                .available(vehicle.isAvailable())
                .depot(depot)
                .build();
    }

    private ProblemDelivery toProblemDelivery(DeliveryEntity delivery) {
        CustomerEntity customer = delivery.getCustomer();
        Coordinates coordinates = customer == null || customer.getCoordinates() == null
                ? null
                : new Coordinates(customer.getCoordinates().getLat(), customer.getCoordinates().getLng());
        return ProblemDelivery.builder()
                .id(delivery.getId())
                .customerId(customer == null ? null : customer.getId())
                .quantity(delivery.getQuantity())
                .coordinates(coordinates)
                .build();
    }
}
