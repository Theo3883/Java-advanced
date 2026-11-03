package org.vros.devtools.demos;

import lombok.extern.slf4j.Slf4j;
import org.vros.devtools.benchmark.CacheBenchmark;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public final class JmhBenchmarkDemo {

    private JmhBenchmarkDemo() {}

    public static void run(ApplicationContext context) {
        log.info("\n------------------------------------------------------------------");
        log.info(" [DEMO 4] Executing JMH Microbenchmark");
        log.info("------------------------------------------------------------------");
        try {
            if (context instanceof ConfigurableApplicationContext cfgCtx) {
                CacheBenchmark.runBenchmark(cfgCtx);
                log.info("JMH Microbenchmark execution completed successfully!");
            }
        } catch (Exception e) {
            log.warn("JMH run completed with notice: {}", e.getMessage());
        }
    }
}
