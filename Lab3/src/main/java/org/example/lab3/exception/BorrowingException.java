package org.example.lab3.exception;

/** Thrown when a borrowing pre-condition is violated. */
public class BorrowingException extends RuntimeException {
    public BorrowingException(String message) {
        super(message);
    }
}
