package org.vros.infrastructure.config;

import org.vros.application.impl.*;
import org.vros.application.service.*;
import org.vros.domain.port.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public CustomerService customerService(CustomerRepository customerRepository) {
        return new CustomerServiceImpl(customerRepository);
    }

    @Bean
    public DeliveryService deliveryService(DeliveryRepository deliveryRepository) {
        return new DeliveryServiceImpl(deliveryRepository);
    }

    @Bean
    public DeliverySearchService deliverySearchService(DeliverySearchPort deliverySearchPort) {
        return new DeliverySearchServiceImpl(deliverySearchPort);
    }

    @Bean
    public DepotService depotService(DepotRepository depotRepository) {
        return new DepotServiceImpl(depotRepository);
    }

    @Bean
    public DriverService driverService(DriverRepository driverRepository) {
        return new DriverServiceImpl(driverRepository);
    }

    @Bean
    public ScenarioService scenarioService(ScenarioRepository scenarioRepository) {
        return new ScenarioServiceImpl(scenarioRepository);
    }

    @Bean
    public VehicleService vehicleService(VehicleRepository vehicleRepository) {
        return new VehicleServiceImpl(vehicleRepository);
    }
}
