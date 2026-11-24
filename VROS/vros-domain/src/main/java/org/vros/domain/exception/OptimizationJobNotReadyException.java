package org.vros.domain.exception;

import java.util.UUID;

public class OptimizationJobNotReadyException extends RuntimeException {

    public OptimizationJobNotReadyException(UUID jobId, String status) {
        super("Optimization job " + jobId + " is not ready (status " + status + ")");
    }
}
