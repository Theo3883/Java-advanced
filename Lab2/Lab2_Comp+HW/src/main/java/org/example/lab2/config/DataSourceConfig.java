package org.example.lab2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 Configuration properties for the data source.
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
