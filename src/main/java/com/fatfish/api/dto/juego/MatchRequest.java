package com.fatfish.api.dto.juego;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** Finished match sent by the game. {@code comodines} may be omitted when no joker was bought or used. */
public record MatchRequest(
        @NotNull(message = "numeroLocal es obligatorio")
        @Min(value = 1, message = "numeroLocal debe ser mayor o igual a 1")
        Integer numeroLocal,

        @NotNull(message = "resultado es obligatorio")
        @Pattern(regexp = "VICTORIA|DERROTA|ABANDONO", message = "resultado debe ser VICTORIA, DERROTA o ABANDONO")
        String resultado,

        @NotNull(message = "rondaAlcanzada es obligatoria")
        @Min(value = 1, message = "rondaAlcanzada debe estar entre 1 y 9")
        @Max(value = 9, message = "rondaAlcanzada debe estar entre 1 y 9")
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
