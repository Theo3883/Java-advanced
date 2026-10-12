package org.example.lab2.runner;

import org.example.lab2.config.DataSourceConfig;
import org.example.lab2.model.Product;
import org.example.lab2.reader.FileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Profile({"dev", "prod"})
public class DataLoaderRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderRunner.class);

    private final FileReader fileReader;
    private final DataSourceConfig config;
    private final Environment environment;

    public DataLoaderRunner(FileReader fileReader, DataSourceConfig config, Environment environment) {
        this.fileReader = fileReader;
        this.config = config;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Active profiles : {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("  Reader type     : {}", fileReader.getReaderType());
        log.info("  Config          : {}", config);
        log.info("═══════════════════════════════════════════════════════════");

        List<Product> products = fileReader.loadProducts();

        log.info("Loaded {} product(s) from '{}':", products.size(), config.getFilePath());
        products.forEach(p -> log.info("  → {}", p));

        log.info("═══════════════════════════════════════════════════════════");

        // Demonstrate configuration priority:
        // CLI arg --server.port > env var SERVER_PORT > application-{profile}.yml > application.properties
        String port = environment.getProperty("server.port");
        String appName = environment.getProperty("spring.application.name");
        log.info("  server.port (resolved, shows priority chain) : {}", port);
        log.info("  spring.application.name                      : {}", appName);
        log.info("═══════════════════════════════════════════════════════════");
    }
}
