package org.example.lab3.drools.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

/**
 * Builds the Drools {@link KieContainer} programmatically.
 */
@Configuration
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices   ks  = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        // Discover every *.drl file on the classpath under rules/
        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] drlFiles = resolver.getResources("classpath*:rules/**/*.drl");

        if (drlFiles.length == 0) {
            throw new IllegalStateException(
                "[Drools] No *.drl files found under classpath:rules/ — check the build output.");
        }

        for (Resource drl : drlFiles) {
            // Drools KieFileSystem path convention: src/main/resources/<dir>/<file>
            String kfsPath = "src/main/resources/rules/" + drl.getFilename();
            kfs.write(kfsPath,
                      ks.getResources().newInputStreamResource(drl.getInputStream()));
            log.info("[Drools] Registered rule file: '{}'", drl.getFilename());
        }

        // Build all rules — fails fast on DRL compilation errors
        KieBuilder builder = ks.newKieBuilder(kfs).buildAll();

        if (builder.getResults().hasMessages(Message.Level.ERROR)) {
            String errors = builder.getResults()
                                   .getMessages(Message.Level.ERROR)
                                   .stream()
                                   .map(m -> "  [%s:%d] %s".formatted(m.getPath(), m.getLine(), m.getText()))
                                   .reduce("", (a, b) -> a + "\n" + b);
            throw new IllegalStateException("[Drools] DRL compilation failed:" + errors);
        }

        KieModule    module    = builder.getKieModule();
        KieContainer container = ks.newKieContainer(module.getReleaseId());

        int ruleCount = container.getKieBase()
                                 .getKiePackages()
                                 .stream()
                                 .mapToInt(p -> p.getRules().size())
                                 .sum();
        log.info("[Drools] KieContainer ready — {} rule(s) loaded from {} file(s).",
                 ruleCount, drlFiles.length);

        return container;
    }
}
