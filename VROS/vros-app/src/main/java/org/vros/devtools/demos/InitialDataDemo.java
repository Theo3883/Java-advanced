package org.vros.devtools.demos;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.vros.domain.model.Customer;
import org.vros.domain.model.Delivery;
import org.vros.domain.model.Depot;
import org.vros.domain.model.Driver;
import org.vros.domain.model.Vehicle;
import org.vros.domain.model.Coordinates;
import org.vros.application.service.CustomerService;
import org.vros.application.service.DeliveryService;
import org.vros.application.service.DepotService;
import org.vros.application.service.DriverService;
import org.vros.application.service.VehicleService;

import java.time.LocalDateTime;

@Slf4j
public final class InitialDataDemo {

    private InitialDataDemo() {}

    public static void run(DepotService depotService,
                           DriverService driverService,
                           CustomerService customerService,
                           VehicleService vehicleService,
                           DeliveryService deliveryService) {
        if (depotService.findAll().isEmpty()) {
            log.info(">> Database has no records; generating sample records with DataFaker...");
            Faker faker = new Faker();

            Depot depot = depotService.save(Depot.builder()
                    .name("Main Hub " + faker.address().cityName())
                    .coordinates(new Coordinates(44.4323, 26.1063))
                    .build());

            Driver d1 = driverService.save(Driver.builder()
                    .name(faker.name().fullName())
                    .email("demo_driver1@vros.com")
                    .phone("+40700111222")
                    .build());

            Driver d2 = driverService.save(Driver.builder()
                    .name(faker.name().fullName())
                    .email("demo_driver2@vros.com")
                    .phone("+40700333444")
                    .build());

            vehicleService.save(Vehicle.builder()
                    .name("Mercedes Sprinter " + faker.vehicle().model())
                    .capacity(350)
                    .available(true)
                    .depot(depot)
                    .driver(d1)
                    .build());

            vehicleService.save(Vehicle.builder()
                    .name("Volvo FH16")
                    .capacity(800)
                    .available(true)
                    .depot(depot)
                    .driver(d2)
                    .build());

            Customer c1 = customerService.save(Customer.builder()
                    .name("Acme Logistics")
                    .email("contact@acme.example.com")
                    .phone("+40722100200")
                    .address("Splaiul Independentei 313, Bucharest")
                    .coordinates(new Coordinates(44.4350, 26.0470))
                    .build());

            Customer c2 = customerService.save(Customer.builder()
                    .name("Globex Corp")
                    .email("support@globex.example.com")
                    .phone("+40722300400")
                    .address("Calea Victoriei 15, Bucharest")
                    .coordinates(new Coordinates(44.4390, 26.0970))
                    .build());

            deliveryService.save(Delivery.builder()
                    .customer(c1)
                    .quantity(150)
                    .notes("Pallet delivery - urgent")
                    .timestamp(LocalDateTime.now().minusDays(1))
                    .build());

            deliveryService.save(Delivery.builder()
                    .customer(c1)
                    .quantity(45)
                    .notes("Standard carton")
                    .timestamp(LocalDateTime.now())
                    .build());

            deliveryService.save(Delivery.builder()
                    .customer(c2)
                    .quantity(220)
                    .notes("Heavy machinery parts")
                    .timestamp(LocalDateTime.now().minusDays(3))
                    .build());

            log.info(">> Sample records created successfully!");
        } else {
            log.info(">> Existing database data detected ({} depots, {} customers).",
                    depotService.findAll().size(), customerService.findAll().size());
        }
    }
}
