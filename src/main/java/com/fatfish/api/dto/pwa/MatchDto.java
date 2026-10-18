package com.fatfish.api.dto.pwa;

import java.time.Instant;

/**
 * One match as the PWA history shows it ("partida"). {@code creadaEn} keeps its name in the PWA contract
 * but carries {@code terminada_en}, the date the match was played.
 */
public record MatchDto(
        String id,
        String resultado,
        int rondaAlcanzada,
        long puntaje,
        long dineroInvertido,
        long dineroGanado,
        long dineroPerdido,
        long duracionSeg,
        Instant creadaEn) {
}
