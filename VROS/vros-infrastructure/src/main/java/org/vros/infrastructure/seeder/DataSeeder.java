package org.vros.infrastructure.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.vros.infrastructure.persistence.entity.*;
import org.vros.infrastructure.persistence.springdata.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds the database using DataFaker with high volume.
 * Activated on demand via profile: --spring.profiles.active=seed
 */
@Component
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final int DEPOT_COUNT    = 50;
    private static final int DRIVER_COUNT   = 10_000;
    private static final int CUSTOMER_COUNT = 10_000;
    private static final int VEHICLE_COUNT  = 10_000;
    private static final int DELIVERY_COUNT = 50_000;
    private static final int BATCH_SIZE     = 500;

    private static final int[] CAPACITIES = {50, 100, 200, 300, 500};
    private static final double MIN_LAT   = 43.5;
    private static final double MAX_LAT   = 48.5;
    private static final double MIN_LNG   = 21.0;
    private static final double MAX_LNG   = 30.0;

    private final DepotJpaRepository depotRepository;
    private final DriverJpaRepository driverRepository;
    private final CustomerJpaRepository customerRepository;
    private final VehicleJpaRepository vehicleRepository;
    private final DeliveryJpaRepository deliveryRepository;

    @Override
    public void run(String... args) {
        log.info("=== DataSeeder starting with DataFaker (profile=seed) ===");

        Faker faker = new Faker();
        Random random = new Random();
        long timestamp = System.currentTimeMillis();

        // 1. Depots
        log.info("Seeding {} depots...", DEPOT_COUNT);
        List<DepotEntity> depots = new ArrayList<>(DEPOT_COUNT);
        for (int i = 0; i < DEPOT_COUNT; i++) {
            depots.add(DepotEntity.builder()
                    .name("Depot " + faker.address().city() + " " + i)
                    .coordinates(randomCoords(random))
                    .build());
        }
        depots = depotRepository.saveAll(depots);
        log.info(">> Successfully committed {} depots.", depots.size());

        // 2. Drivers
        log.info("Seeding {} drivers in batches of {}...", DRIVER_COUNT, BATCH_SIZE);
        List<DriverEntity> driverBatch = new ArrayList<>(BATCH_SIZE);
        List<DriverEntity> newlyCreatedDrivers = new ArrayList<>(DRIVER_COUNT);
        for (int i = 0; i < DRIVER_COUNT; i++) {
            driverBatch.add(DriverEntity.builder()
                    .name(faker.name().fullName())
                    .email("driver_" + timestamp + "_" + i + "@vros.com")
                    .phone(faker.phoneNumber().cellPhone())
                    .build());
            if (driverBatch.size() == BATCH_SIZE) {
                newlyCreatedDrivers.addAll(driverRepository.saveAll(driverBatch));
                driverBatch.clear();
            }
        }
        if (!driverBatch.isEmpty()) {
            newlyCreatedDrivers.addAll(driverRepository.saveAll(driverBatch));
        }
        log.info(">> Successfully committed {} drivers.", newlyCreatedDrivers.size());

        // 3. Customers
        log.info("Seeding {} customers in batches of {}...", CUSTOMER_COUNT, BATCH_SIZE);
        List<CustomerEntity> customerBatch = new ArrayList<>(BATCH_SIZE);
        List<CustomerEntity> newlyCreatedCustomers = new ArrayList<>(CUSTOMER_COUNT);
        for (int i = 0; i < CUSTOMER_COUNT; i++) {
            customerBatch.add(CustomerEntity.builder()
                    .name(faker.name().fullName())
                    .email("customer_" + timestamp + "_" + i + "@vros.com")
                    .phone(faker.phoneNumber().cellPhone())
                    .address(faker.address().fullAddress())
                    .coordinates(randomCoords(random))
                    .build());
            if (customerBatch.size() == BATCH_SIZE) {
                newlyCreatedCustomers.addAll(customerRepository.saveAll(customerBatch));
                customerBatch.clear();
            }
        }
        if (!customerBatch.isEmpty()) {
            newlyCreatedCustomers.addAll(customerRepository.saveAll(customerBatch));
        }
        log.info(">> Successfully committed {} customers.", newlyCreatedCustomers.size());

        // 4. Vehicles
        log.info("Seeding {} vehicles in batches of {}...", VEHICLE_COUNT, BATCH_SIZE);
        List<VehicleEntity> vehicleBatch = new ArrayList<>(BATCH_SIZE);
        int driverIdx = 0;
        int vehiclesCommitted = 0;
        for (int i = 0; i < VEHICLE_COUNT; i++) {
            DepotEntity depot = depots.get(i % depots.size());
            DriverEntity driver = (random.nextDouble() < 0.7 && driverIdx < newlyCreatedDrivers.size())
                    ? newlyCreatedDrivers.get(driverIdx++) : null;

            vehicleBatch.add(VehicleEntity.builder()
                    .name(faker.vehicle().make() + "-" + faker.vehicle().model() + "-" + i)
                    .capacity(CAPACITIES[random.nextInt(CAPACITIES.length)])
                    .available(random.nextDouble() > 0.15)
                    .depot(depot)
                    .driver(driver)
                    .build());
            if (vehicleBatch.size() == BATCH_SIZE) {
                vehicleRepository.saveAll(vehicleBatch);
                vehiclesCommitted += vehicleBatch.size();
                vehicleBatch.clear();
            }
        }
        if (!vehicleBatch.isEmpty()) {
            vehicleRepository.saveAll(vehicleBatch);
            vehiclesCommitted += vehicleBatch.size();
        }
        log.info(">> Successfully committed {} vehicles.", vehiclesCommitted);

        // 5. Deliveries
        log.info("Seeding {} deliveries in batches of {}...", DELIVERY_COUNT, BATCH_SIZE);
        List<DeliveryEntity> deliveryBatch = new ArrayList<>(BATCH_SIZE);
        int deliveriesCommitted = 0;
        for (int i = 0; i < DELIVERY_COUNT; i++) {
            CustomerEntity customer = newlyCreatedCustomers.get(random.nextInt(newlyCreatedCustomers.size()));
            deliveryBatch.add(DeliveryEntity.builder()
                    .customer(customer)
                    .quantity(10 + random.nextInt(491))
                    .notes(random.nextDouble() > 0.5 ? faker.lorem().sentence() : null)
                    .timestamp(LocalDateTime.now()
                            .minusDays(random.nextInt(365))
                            .minusHours(random.nextInt(24)))
                    .build());
            if (deliveryBatch.size() == BATCH_SIZE) {
                deliveryRepository.saveAll(deliveryBatch);
                deliveriesCommitted += deliveryBatch.size();
                deliveryBatch.clear();
            }
        }
        if (!deliveryBatch.isEmpty()) {
            deliveryRepository.saveAll(deliveryBatch);
            deliveriesCommitted += deliveryBatch.size();
        }
        log.info(">> Successfully committed {} deliveries.", deliveriesCommitted);

        log.info("=== DataSeeder finished successfully: all records committed to PostgreSQL ===");
    }

    private CoordinatesEmbeddable randomCoords(Random random) {
        double lat = MIN_LAT + random.nextDouble() * (MAX_LAT - MIN_LAT);
        double lng = MIN_LNG + random.nextDouble() * (MAX_LNG - MIN_LNG);
        return new CoordinatesEmbeddable(lat, lng);
    }
}
