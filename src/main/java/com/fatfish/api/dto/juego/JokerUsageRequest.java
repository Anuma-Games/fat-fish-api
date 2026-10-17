package com.fatfish.api.dto.juego;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record JokerUsageRequest(
        @NotNull(message = "comodinId es obligatorio")
        @Min(value = 1, message = "comodinId debe ser un comodín válido")
        @Max(value = Short.MAX_VALUE, message = "comodinId debe ser un comodín válido")
        Integer comodinId,

        @NotNull(message = "vecesComprado es obligatorio")
        @Min(value = 0, message = "vecesComprado no puede ser negativo")
        Integer vecesComprado,

        @NotNull(message = "vecesUsado es obligatorio")
        @Min(value = 0, message = "vecesUsado no puede ser negativo")
        Integer vecesUsado) {
}
