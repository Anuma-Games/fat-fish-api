package com.fatfish.api.dto.juego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "contrasenaNueva es obligatoria")
        @Size(min = 8, max = 72, message = "contrasenaNueva debe tener entre 8 y 72 caracteres")
        String contrasenaNueva) {
}
