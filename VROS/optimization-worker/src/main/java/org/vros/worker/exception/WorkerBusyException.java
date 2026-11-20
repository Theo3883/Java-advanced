package org.vros.worker.exception;

/**
 * Thrown when the bounded solver pool rejects a task. Retryable: {@code @RetryableTopic} will
 * redeliver the request, and the request eventually lands in the DLT if the worker stays busy.
 */
public class WorkerBusyException extends RuntimeException {

    public WorkerBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
