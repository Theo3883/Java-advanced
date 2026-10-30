package org.example.vros.runner.demos;

import lombok.extern.slf4j.Slf4j;
import org.example.vros.model.Customer;
import org.example.vros.model.Driver;
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

        List<Customer> allCustomers = customerService.findAll();

        // 1. Derived Query on Customers
        String keyword = allCustomers.isEmpty() ? "Acme"
                : allCustomers.get(0).getName().substring(0, Math.min(4, allCustomers.get(0).getName().length()));
        List<Customer> matchingCustomers = customerService.searchByName(keyword);
        log.info("Derived Query - searchByName('{}'): found {} results", keyword, matchingCustomers.size());

        // 2. Derived Query on Vehicles
        List<Vehicle> availableVehicles = vehicleService.findAvailable();
        log.info("Derived Query - findAvailableVehicles(): found {} available vehicles", availableVehicles.size());

        // 3. JPQL Query on Vehicles
        List<Vehicle> heavyVehicles = vehicleService.findAvailableWithMinCapacity(300);
        log.info("JPQL Query - findAvailableWithMinCapacity(300): found {} vehicles", heavyVehicles.size());

        // 4. JPQL Sum Query on Deliveries
        if (!allCustomers.isEmpty()) {
            Long customerId = allCustomers.get(0).getId();
            Integer totalQuantity = deliveryService.totalQuantityForCustomer(customerId);
            log.info("JPQL Query - totalQuantityForCustomer({}): {} total units demanded",
                    customerId, totalQuantity != null ? totalQuantity : 0);
        }

        // 5. Transactional Modifying Query on Drivers
        List<Driver> allDrivers = driverService.findAll();
        if (!allDrivers.isEmpty()) {
            String targetEmail = allDrivers.get(0).getEmail();
            int updated = driverService.updatePhone(targetEmail, "+40799888777");
            log.info("Transactional Modifying Query - updatePhone('{}'): updated {} row(s)", targetEmail, updated);
        }
    }
}
