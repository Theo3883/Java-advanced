package org.vros.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OptimizationWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OptimizationWorkerApplication.class, args);
    }
}
