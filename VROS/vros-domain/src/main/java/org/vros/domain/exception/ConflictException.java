package org.vros.domain.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public ConflictException(String email) {
        super("Email already in use: " + email);
        this.messageKey = "error.conflict.email";
        this.args = new Object[]{email};
    }

    public ConflictException(String messageKey, String email, String fallbackMessage) {
        super(fallbackMessage);
        this.messageKey = messageKey;
        this.args = new Object[]{email};
    }
}
