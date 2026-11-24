package org.vros.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "vros.worker")
public class WorkerProperties {

    private final Pool pool = new Pool();
    private int listenerConcurrency = 3;
    private long simulatedDelayMs = 0;
    private int maxSolverAttempts = 3;
    private long retryBackoffMs = 250;
    private long progressThrottleMs = 250;

    @Getter
    @Setter
    public static class Pool {
        private int coreSize = 2;
        private int maxSize = 4;
        private int queueCapacity = 50;
    }
}
