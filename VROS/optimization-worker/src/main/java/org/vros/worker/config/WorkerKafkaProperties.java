package org.vros.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.vros.contracts.optimization.OptimizationTopics;

@Getter
@Setter
@ConfigurationProperties(prefix = "vros.kafka")
public class WorkerKafkaProperties {

    private final Topics topics = new Topics();
    private final Groups groups = new Groups();

    @Getter
    @Setter
    public static class Topics {
        private String requests = OptimizationTopics.REQUESTS;
        private String acks = OptimizationTopics.ACKS;
        private String results = OptimizationTopics.RESULTS;
        private String progress = OptimizationTopics.PROGRESS;
        private String requestsDlt = OptimizationTopics.REQUESTS_DLT;
    }

    @Getter
    @Setter
    public static class Groups {
        private String worker = "optimization-worker";
        private String workerDlq = "optimization-worker-dlq";
    }
}
