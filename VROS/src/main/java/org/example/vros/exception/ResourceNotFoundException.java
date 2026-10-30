package org.example.vros.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " with id " + id + " not found");
        this.messageKey = "error.resource.not_found";
        this.args = new Object[]{resourceName, String.valueOf(id)};
    }

    public ResourceNotFoundException(String messageKey, String fallbackMessage) {
        super(fallbackMessage);
        this.messageKey = messageKey;
        this.args = null;
    }

    public ResourceNotFoundException(String fallbackMessage) {
        super(fallbackMessage);
        this.messageKey = "error.resource.not_found.generic";
        this.args = new Object[]{fallbackMessage};
    }
}
