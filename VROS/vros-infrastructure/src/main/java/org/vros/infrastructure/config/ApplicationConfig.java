package org.vros.infrastructure.config;

import org.vros.application.impl.*;
import org.vros.application.port.OptimizationEventPublisher;
import org.vros.application.port.OptimizationProgressNotifier;
import org.vros.application.service.*;
import org.vros.contracts.optimization.OptimizationProgressEvent;
import org.vros.domain.model.OptimizationJob;
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

    @Bean
    public OptimizationService optimizationService(
            OptimizationJobRepository jobRepository,
            OptimizationSolutionRepository solutionRepository,
            OptimizationProblemRepository problemRepository,
            OptimizationEventPublisher eventPublisher,
            OptimizationProgressNotifier progressNotifier) {
        return new OptimizationServiceImpl(
                jobRepository, solutionRepository, problemRepository, eventPublisher, progressNotifier);
    }

    /**
     * Fallback notifier for non-web slices and tests. The STOMP broadcaster in the API module is
     * marked {@code @Primary}, so the WebSocket implementation wins when it is present.
     */
    @Bean
    public OptimizationProgressNotifier noOpOptimizationProgressNotifier() {
        return new OptimizationProgressNotifier() {
            @Override
            public void notifyProgress(OptimizationProgressEvent event) {
                // no-op
            }

            @Override
            public void notifyTerminal(OptimizationJob job) {
                // no-op
            }
        };
    }
}
