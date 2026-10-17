package com.fatfish.api.exception;

/** Login with an id that does not exist or a wrong password; both get the same message (401). */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("El id del jugador o la contraseña no son correctos");
    }
}
