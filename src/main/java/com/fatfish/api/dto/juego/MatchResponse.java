package com.fatfish.api.dto.juego;

import java.time.Instant;
import java.util.UUID;

/** Server-generated id (as text) of a stored match, with the key the game sent. */
public record MatchResponse(String id, UUID claveIdempotencia, Instant creadaEn) {
}
