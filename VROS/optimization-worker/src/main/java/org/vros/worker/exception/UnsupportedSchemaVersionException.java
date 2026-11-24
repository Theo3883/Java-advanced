package org.vros.worker.exception;

/**
 * Thrown for events the worker cannot understand. It is non-retryable and is excluded from
 * {@code @RetryableTopic}, so the message goes straight to the dead-letter topic.
 */
public class UnsupportedSchemaVersionException extends RuntimeException {

    public UnsupportedSchemaVersionException(int actual, int expected) {
        super("Unsupported optimization event schema version " + actual + " (expected " + expected + ")");
    }
}
