package org.example.lab2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Homework: Configuration properties for the data source.
 *
 * WHY @ConfigurationProperties over @Value?
 * ─────────────────────────────────────────
 * @ConfigurationProperties is preferred here because:
 *  1. There are multiple related properties (file-path, file-format, delimiter, has-header)
 *     that logically belong together — grouping them into a single POJO avoids scattering
 *     @Value annotations across the codebase.
 *  2. It supports type-safe binding (booleans, chars, enums) without manual conversion.
 *  3. It enables IDE autocompletion via the spring-boot-configuration-processor.
 *  4. It works seamlessly with @Profile-specific YAML files — Spring merges properties
 *     from application.properties + application-{profile}.yml automatically.
 *  5. It is easier to validate with @Validated / JSR-303 annotations.
 *
 * @Value is used in HelloController because it reads a single, simple property
 * that needs no grouping or validation — the ideal use case for @Value.
 */
@Component
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceConfig {

    private String filePath;

    private String fileFormat;

    private String delimiter = ",";

    private boolean hasHeader = true;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }

    public String getDelimiter() { return delimiter; }
    public void setDelimiter(String delimiter) { this.delimiter = delimiter; }

    public boolean isHasHeader() { return hasHeader; }
    public void setHasHeader(boolean hasHeader) { this.hasHeader = hasHeader; }

    @Override
    public String toString() {
        return String.format("DataSourceConfig{filePath='%s', fileFormat='%s', delimiter='%s', hasHeader=%b}",
                filePath, fileFormat, delimiter, hasHeader);
    }
}
