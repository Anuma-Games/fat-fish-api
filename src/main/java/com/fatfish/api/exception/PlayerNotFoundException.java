package com.fatfish.api.exception;

import java.util.UUID;

public class PlayerNotFoundException extends RuntimeException {

    private final UUID playerId;

    public PlayerNotFoundException(UUID playerId) {
        super("No existe el jugador " + playerId);
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
