package com.fatfish.api.dto.juego;

/**
 * Id of the player (as text, like in the PWA contract) and its token. The token is returned only
 * here; the database keeps its SHA-256, so the game must store it on the device.
 */
public record SessionResponse(String jugadorId, String token) {
}
