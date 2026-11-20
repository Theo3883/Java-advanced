package org.vros.contracts.optimization;

/**
 * Default Kafka topic names used by VROS and the OptimizationWorker.
 * Both applications allow overriding these through configuration.
 */
public final class OptimizationTopics {

    public static final String REQUESTS = "vros.optimization.requests";
    public static final String ACKS = "vros.optimization.acks";
    public static final String RESULTS = "vros.optimization.results";
    public static final String PROGRESS = "vros.optimization.progress";
    public static final String REQUESTS_DLT = REQUESTS + "-dlt";
    public static final String RESULTS_DLT = RESULTS + "-dlt";

    private OptimizationTopics() {
    }
}
