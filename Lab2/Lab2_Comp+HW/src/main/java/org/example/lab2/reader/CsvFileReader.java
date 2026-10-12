package org.example.lab2.reader;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.example.lab2.config.DataSourceConfig;
import org.example.lab2.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 CSV-based file reader, active only when:
 1. The "dev" profile is active — @Profile("dev")
 2. AND the configured file format is "csv" — @ConditionalOnExpression
 */
@Component
@Profile("dev")
@ConditionalOnExpression("'${app.datasource.file-format:csv}' == 'csv'")
public class CsvFileReader implements FileReader {

    private static final Logger log = LoggerFactory.getLogger(CsvFileReader.class);

    private final DataSourceConfig config;

    public CsvFileReader(DataSourceConfig config) {
        this.config = config;
        log.info("CsvFileReader created — will read from: {}", config.getFilePath());
    }

    @Override
    public List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();

        try {
            ClassPathResource resource = new ClassPathResource(config.getFilePath());
            try (CSVReader csvReader = new CSVReaderBuilder(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                    .withSkipLines(config.isHasHeader() ? 1 : 0)   // skip header row if configured
                    .build()) {

                String[] row;
                while ((row = csvReader.readNext()) != null) {
                    if (row.length >= 4) {
                        products.add(new Product(
                                Integer.parseInt(row[0].trim()),
                                row[1].trim(),
                                Double.parseDouble(row[2].trim()),
                                row[3].trim()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to load CSV from {}: {}", config.getFilePath(), e.getMessage(), e);
        }

        log.info("CsvFileReader loaded {} products from {}", products.size(), config.getFilePath());
        return products;
    }

    @Override
    public String getReaderType() {
        return "CSV Reader (dev profile)";
    }
}
