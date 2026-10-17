package com.fatfish.api.dto.juego;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "jugadorId es obligatorio")
        @Min(value = 1, message = "jugadorId debe ser un número de jugador válido")
        Long jugadorId,

        @NotBlank(message = "contrasena es obligatoria")
        String contrasena) {
}
