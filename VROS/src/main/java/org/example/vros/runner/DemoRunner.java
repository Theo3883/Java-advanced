package org.example.vros.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vros.runner.demos.CachePerformanceDemo;
import org.example.vros.runner.demos.CriteriaSearchDemo;
import org.example.vros.runner.demos.InitialDataDemo;
import org.example.vros.runner.demos.JmhBenchmarkDemo;
import org.example.vros.runner.demos.ServiceQueriesDemo;
import org.example.vros.service.interfaces.CustomerService;
import org.example.vros.service.interfaces.DeliverySearchService;
import org.example.vros.service.interfaces.DeliveryService;
import org.example.vros.service.interfaces.DepotService;
import org.example.vros.service.interfaces.DriverService;
import org.example.vros.service.interfaces.VehicleService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Main coordinator CommandLineRunner for VROS demonstrations.
 * Delegates each demo example to its dedicated class in org.example.vros.runner.demos.
 */
@Component
@Profile("!seed & !test")
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DemoRunner implements CommandLineRunner {

    private final DepotService depotService;
    private final DriverService driverService;
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final DeliveryService deliveryService;
    private final DeliverySearchService deliverySearchService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        log.info("==================================================================");
        log.info("             VROS - FULL DEMO & BENCHMARK RUNNER                  ");
        log.info("==================================================================");

        // Step 0: Ensure database has sample records
        InitialDataDemo.run(depotService, driverService, customerService, vehicleService, deliveryService);

        // Demo 1: SOLID Service Layer & Repository Queries
        ServiceQueriesDemo.run(driverService, customerService, vehicleService, deliveryService);

        // Demo 2: Dynamic Criteria API Searches with multiple filters
        CriteriaSearchDemo.run(deliverySearchService);

        // Demo 3: Application-level Caching Performance (Cold vs Warm)
        CachePerformanceDemo.run(deliveryService, applicationContext);

        // Demo 4: JMH Microbenchmark Execution
        JmhBenchmarkDemo.run(applicationContext);

        log.info("==================================================================");
        log.info("             VROS DEMO & BENCHMARK COMPLETED                      ");
        log.info("==================================================================");
    }
}
