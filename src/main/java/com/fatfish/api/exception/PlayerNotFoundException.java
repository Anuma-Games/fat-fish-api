package com.fatfish.api.exception;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(long playerId) {
        super("No existe el jugador #" + playerId);
    }
}
