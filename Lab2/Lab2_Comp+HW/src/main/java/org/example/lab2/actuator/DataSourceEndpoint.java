package org.example.lab2.actuator;

import org.example.lab2.config.DataSourceConfig;
import org.example.lab2.model.Product;
import org.example.lab2.reader.FileReader;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns metadata about the currently active data source:
 *  - active profile(s)
 *  - file path & format
 *  - reader implementation type
 *  - number of records loaded
 *  - sample records (first 3)
 *
 * Active only when a "dev" or "prod" profile is set (skipped in plain test context).
 */
@Component
@Endpoint(id = "datasource")
@Profile({"dev", "prod"})
public class DataSourceEndpoint {

    private final DataSourceConfig config;
    private final FileReader fileReader;
    private final Environment environment;

    public DataSourceEndpoint(DataSourceConfig config, FileReader fileReader, Environment environment) {
        this.config = config;
        this.fileReader = fileReader;
        this.environment = environment;
    }

    /**
     * HTTP GET /actuator/datasource
     * Returns a JSON map describing the active data source configuration and loaded data.
     */
    @ReadOperation
    public Map<String, Object> datasourceInfo() {
        List<Product> products = fileReader.loadProducts();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("activeProfiles", Arrays.toString(environment.getActiveProfiles()));
        info.put("readerType", fileReader.getReaderType());
        info.put("filePath", config.getFilePath());
        info.put("fileFormat", config.getFileFormat());
        info.put("delimiter", config.getDelimiter());
        info.put("hasHeader", config.isHasHeader());
        info.put("recordCount", products.size());
        info.put("sampleRecords", products.stream().limit(3)
                .map(p -> Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "price", p.getPrice(),
                        "category", p.getCategory()))
                .toList());

        return info;
    }
}
