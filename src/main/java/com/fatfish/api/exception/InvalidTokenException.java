package com.fatfish.api.exception;

/** The Authorization header is missing or its token does not belong to any player (401). */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("El token no es válido o no pertenece a ningún jugador");
    }
}
