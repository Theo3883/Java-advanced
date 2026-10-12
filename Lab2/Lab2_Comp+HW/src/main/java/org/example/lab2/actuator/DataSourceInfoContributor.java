package org.example.lab2.actuator;

import org.example.lab2.config.DataSourceConfig;
import org.example.lab2.reader.FileReader;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * InfoContributor that adds datasource metadata to /actuator/info.
 *
 * Unlike the custom @Endpoint (which has its own URL), an InfoContributor
 * adds a section to the existing /actuator/info response — useful for
 * lightweight status that is always available alongside app version / git info.
 */
@Component
@Profile({"dev", "prod"})
public class DataSourceInfoContributor implements InfoContributor {

    private final DataSourceConfig config;
    private final FileReader fileReader;
    private final Environment environment;

    public DataSourceInfoContributor(DataSourceConfig config, FileReader fileReader, Environment environment) {
        this.config = config;
        this.fileReader = fileReader;
        this.environment = environment;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("datasource", Map.of(
                "activeProfiles", Arrays.toString(environment.getActiveProfiles()),
                "filePath", config.getFilePath(),
                "fileFormat", config.getFileFormat(),
                "readerType", fileReader.getReaderType()
        ));
    }
}
