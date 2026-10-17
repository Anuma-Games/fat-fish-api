package com.fatfish.api.dto.juego;

import java.time.Instant;

/** Server-generated id (as text) of a stored match, with the local number the game sent. */
public record MatchResponse(String id, int numeroLocal, Instant creadaEn) {
}
