package com.fatfish.api.dto.pwa;

import java.time.Instant;

public record PlayerSummaryDto(String jugadorId, String alias, Instant ultimaActividad) {
}