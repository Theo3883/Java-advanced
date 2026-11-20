package org.vros.domain.model;

public enum OptimizationJobStatus {
    PENDING,
    QUEUED,
    ACCEPTED,
    RUNNING,
    RETRYING,
    COMPLETED,
    FAILED,
    SUBMISSION_FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == SUBMISSION_FAILED;
    }
}
