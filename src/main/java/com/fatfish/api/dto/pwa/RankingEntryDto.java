package com.fatfish.api.dto.pwa;

import java.time.Instant;

public record RankingEntryDto(
    long posicion, String jugadorId, String alias, int puntaje, Instant logradoEn) {
}