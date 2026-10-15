package com.fatfish.api.dto.pwa;

import java.time.Instant;

/** One run as the PWA history shows it ("partida"). */
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
