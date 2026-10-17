package com.fatfish.api.exception;

/** A match that passes the field validations but breaks a rule checked by the service (400). */
public class InvalidMatchException extends RuntimeException {

    public InvalidMatchException(String message) {
        super(message);
    }
}
