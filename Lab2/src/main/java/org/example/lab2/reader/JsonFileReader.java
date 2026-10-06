package org.example.lab2.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.lab2.config.DataSourceConfig;
import org.example.lab2.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 JSON-based file reader, active only when:
 1. The "prod" profile is active — @Profile("prod")
 2. AND the configured file format is "json" — @ConditionalOnExpression (SpEL)
 */
@Component
@Profile("prod")
@ConditionalOnExpression("'${app.datasource.file-format:json}' == 'json'")
public class JsonFileReader implements FileReader {

    private static final Logger log = LoggerFactory.getLogger(JsonFileReader.class);

    private final DataSourceConfig config;
    private final ObjectMapper objectMapper;

    public JsonFileReader(DataSourceConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        log.info("JsonFileReader created — will read from: {}", config.getFilePath());
    }

    @Override
    public List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();

        try {
            ClassPathResource resource = new ClassPathResource(config.getFilePath());
            try (InputStream inputStream = resource.getInputStream()) {
                products = objectMapper.readValue(inputStream, new TypeReference<List<Product>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to load JSON from {}: {}", config.getFilePath(), e.getMessage(), e);
        }

        log.info("JsonFileReader loaded {} products from {}", products.size(), config.getFilePath());
        return products;
    }

    @Override
    public String getReaderType() {
        return "JSON Reader (prod profile)";
    }
}
