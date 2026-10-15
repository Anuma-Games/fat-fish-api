package com.fatfish.api.exception;

import java.util.UUID;

public class PlayerNotFoundException extends RuntimeException {

    private final UUID playerId;

    public PlayerNotFoundException(UUID playerId) {
        super("No existe el jugador " + playerId);
        this.playerId = playerId;
    }

    /** Player of the PWA (table jugador), looked up by its numeric id. */
    public PlayerNotFoundException(long number) {
        super("No existe el jugador #" + number);
        this.playerId = null;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
