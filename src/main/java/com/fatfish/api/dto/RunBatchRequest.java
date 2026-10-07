package com.fatfish.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Batch sent by the game's outbox. Wrapped in an object (not a bare array) because
 * Unity's JsonUtility cannot serialize top-level arrays.
 * Records are validated one by one in the service, so one bad record does not reject the batch.
 */
public record RunBatchRequest(
        @NotEmpty(message = "El lote debe contener al menos una partida")
        @Size(max = RunBatchRequest.MAX_SIZE, message = "El lote no puede tener más de 100 partidas")
        @Schema(description = "Partidas a sincronizar (máximo 100)")
        List<RunRecord> runs) {

    public static final int MAX_SIZE = 100;
}
