package com.fatfish.api.dto.pwa;

import java.time.Instant;

/** Player profile for the PWA ("perfil"): public data plus the six statistics of the record. */
public record ProfileDto(
        String jugadorId,
        String alias,
        Instant creadoEn,
        Instant ultimaActividad,
        Statistics estadisticas,
        Instant generadoEn) {

    public record Statistics(
            long partidasTotales,
            long partidasGanadas,
            long totalInvertido,
            long totalPerdido,
            int rachaDerrotasMax,
            long tiempoJugadoSeg) {
    }
}
