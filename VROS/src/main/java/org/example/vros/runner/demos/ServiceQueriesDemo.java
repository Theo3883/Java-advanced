package org.example.vros.runner.demos;

import lombok.extern.slf4j.Slf4j;
import org.example.vros.model.Customer;
import org.example.vros.model.Vehicle;
import org.example.vros.service.interfaces.CustomerService;
import org.example.vros.service.interfaces.DeliveryService;
import org.example.vros.service.interfaces.DriverService;
import org.example.vros.service.interfaces.VehicleService;

import java.util.List;

@Slf4j
public final class ServiceQueriesDemo {

    private ServiceQueriesDemo() {}

    public static void run(DriverService driverService,
                           CustomerService customerService,
                           VehicleService vehicleService,
                           DeliveryService deliveryService) {
        log.info("\n------------------------------------------------------------------");
        log.info(" [DEMO 1] Service Layer & Repository Queries");
        log.info("------------------------------------------------------------------");

        // Derived Query on Customers
        List<Customer> matchingCustomers = customerService.searchByName("Acme");
        log.info("Derived Query - searchByName('Acme'): found {} results", matchingCustomers.size());

        // Derived Query on Vehicles
        List<Vehicle> availableVehicles = vehicleService.findAvailable();
        log.info("Derived Query - findAvailableVehicles(): found {} available vehicles", availableVehicles.size());

        // JPQL Query on Vehicles
        List<Vehicle> heavyVehicles = vehicleService.findAvailableWithMinCapacity(300);
        log.info("JPQL Query - findAvailableWithMinCapacity(300): found {} vehicles", heavyVehicles.size());

        // JPQL Sum Query on Deliveries
        if (!matchingCustomers.isEmpty()) {
            Long customerId = matchingCustomers.get(0).getId();
            Integer totalQuantity = deliveryService.totalQuantityForCustomer(customerId);
            log.info("JPQL Query - totalQuantityForCustomer({}): {} total units demanded", customerId, totalQuantity);
        }

        // Transactional Modifying Query on Drivers
        int updated = driverService.updatePhone("demo_driver1@vros.com", "+40799888777");
        log.info("Transactional Modifying Query - updatePhone(): updated {} row(s)", updated);
    }
}
