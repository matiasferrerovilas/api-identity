package com.api.identity.exceptions;

public final class EntityAlreadyExistsException extends DomainException {
    public EntityAlreadyExistsException(String message) {
        super(message);
    }

    public EntityAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
