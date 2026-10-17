package com.fatfish.api.dto.juego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "alias es obligatorio")
        @Size(min = 3, max = 24, message = "alias debe tener entre 3 y 24 caracteres")
        String alias,

        @NotBlank(message = "contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "contrasena debe tener entre 8 y 72 caracteres")
        String contrasena) {
}
