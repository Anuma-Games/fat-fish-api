package com.fatfish.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contract shared with the game (see fat-fish-arquitectura, section 3). */
@Schema(description = "Registro de una partida terminada, generado por el juego")
public record RunRecord(
        @NotNull(message = "schemaVersion es obligatorio")
        @Schema(example = "1")
        Integer schemaVersion,

        @NotNull(message = "runId es obligatorio")
        UUID runId,

        @NotNull(message = "installationId es obligatorio")
        UUID installationId,

        @Schema(nullable = true, description = "Nulo mientras la instalación no esté vinculada a un jugador")
        UUID playerId,

        @NotBlank(message = "gameVersion es obligatorio")
        @Size(max = 20, message = "gameVersion no puede exceder 20 caracteres")
        @Schema(example = "0.3.0")
        String gameVersion,

        @NotNull(message = "startedAt es obligatorio")
        @Schema(example = "2026-10-05T18:20:11Z")
        Instant startedAt,

        @NotNull(message = "endedAt es obligatorio")
        @Schema(example = "2026-10-05T18:41:52Z")
        Instant endedAt,

        @NotNull(message = "result es obligatorio")
        @Pattern(regexp = "defeat|demo_victory|abandoned",
                message = "result debe ser defeat, demo_victory o abandoned")
        @Schema(allowableValues = {"defeat", "demo_victory", "abandoned"}, example = "defeat")
        String result,

        @NotNull(message = "levelReached es obligatorio")
        @Min(value = 1, message = "levelReached debe estar entre 1 y 3")
        @Max(value = 3, message = "levelReached debe estar entre 1 y 3")
        Integer levelReached,

        @NotNull(message = "roundsCleared es obligatorio")
        @PositiveOrZero(message = "roundsCleared no puede ser negativo")
        Integer roundsCleared,

        @NotNull(message = "bossesDefeated es obligatorio")
        @PositiveOrZero(message = "bossesDefeated no puede ser negativo")
        Integer bossesDefeated,

        @NotNull(message = "maxSpinScore es obligatorio")
        @PositiveOrZero(message = "maxSpinScore no puede ser negativo")
        Long maxSpinScore,

        @NotNull(message = "totalSpins es obligatorio")
        @PositiveOrZero(message = "totalSpins no puede ser negativo")
        Integer totalSpins,

        @NotNull(message = "totalBet es obligatorio")
        @PositiveOrZero(message = "totalBet no puede ser negativo")
        Long totalBet,

        @NotNull(message = "totalWon es obligatorio")
        @PositiveOrZero(message = "totalWon no puede ser negativo")
        Long totalWon,

        @NotNull(message = "totalLost es obligatorio")
        @PositiveOrZero(message = "totalLost no puede ser negativo")
        Long totalLost,

        @NotNull(message = "allInCount es obligatorio")
        @PositiveOrZero(message = "allInCount no puede ser negativo")
        Integer allInCount,

        List<@NotBlank(message = "pawnedAssets no puede contener valores vacíos") String> pawnedAssets,

        @NotNull(message = "loansTaken es obligatorio")
        @PositiveOrZero(message = "loansTaken no puede ser negativo")
        Integer loansTaken,

        @NotNull(message = "loanTotal es obligatorio")
        @PositiveOrZero(message = "loanTotal no puede ser negativo")
        Long loanTotal,

        @Valid
        DesperateSpinDto mostDesperateSpin,

        List<@NotBlank(message = "itemsBought no puede contener valores vacíos")
             @Size(max = 50, message = "itemsBought contiene un código de más de 50 caracteres") String> itemsBought) {
}
