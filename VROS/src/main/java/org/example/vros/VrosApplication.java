package org.example.vros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = org.springdoc.core.configuration.SpringDocHateoasConfiguration.class)
@EnableCaching
public class VrosApplication {

    public static void main(String[] args) {
        SpringApplication.run(VrosApplication.class, args);
    }

}
