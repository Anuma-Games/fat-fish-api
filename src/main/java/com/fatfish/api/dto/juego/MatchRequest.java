package com.fatfish.api.dto.juego;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Finished match sent by the game. {@code claveIdempotencia} is the UUID the game generated when the match
 * ended and repeats on every retry. {@code comodines} may be omitted when no joker was bought or used.
 */
public record MatchRequest(
        @NotNull(message = "claveIdempotencia es obligatoria")
        UUID claveIdempotencia,

        @NotNull(message = "terminadaEn es obligatoria")
        Instant terminadaEn,

        @NotNull(message = "resultado es obligatorio")
        @Pattern(regexp = "VICTORIA|DERROTA|ABANDONO", message = "resultado debe ser VICTORIA, DERROTA o ABANDONO")
        String resultado,

        @NotNull(message = "rondaAlcanzada es obligatoria")
        @Min(value = 1, message = "rondaAlcanzada debe ser al menos 1")
        Integer rondaAlcanzada,

        @NotNull(message = "puntaje es obligatorio")
        @Min(value = 0, message = "puntaje no puede ser negativo")
        Integer puntaje,

        @NotNull(message = "dineroInvertido es obligatorio")
        @Min(value = 0, message = "dineroInvertido no puede ser negativo")
        Integer dineroInvertido,

        @NotNull(message = "dineroGanado es obligatorio")
        @Min(value = 0, message = "dineroGanado no puede ser negativo")
        Integer dineroGanado,

        @NotNull(message = "dineroPerdido es obligatorio")
        @Min(value = 0, message = "dineroPerdido no puede ser negativo")
        Integer dineroPerdido,

        @NotNull(message = "vecesTodoONada es obligatorio")
        @Min(value = 0, message = "vecesTodoONada no puede ser negativo")
        Integer vecesTodoONada,

        @NotNull(message = "empenos es obligatorio")
        @Min(value = 0, message = "empenos no puede ser negativo")
        Integer empenos,

        @NotNull(message = "prestamos es obligatorio")
        @Min(value = 0, message = "prestamos no puede ser negativo")
        Integer prestamos,

        @NotNull(message = "duracionSeg es obligatoria")
        @Min(value = 0, message = "duracionSeg no puede ser negativa")
        Integer duracionSeg,

        List<@Valid @NotNull(message = "comodines no puede tener elementos vacíos") JokerUsageRequest> comodines) {
}
