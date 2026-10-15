package com.fatfish.api.dto.pwa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotNull(message = "categoria es obligatoria")
        @Pattern(regexp = "ERROR|SUGERENCIA|OTRO", message = "categoria debe ser ERROR, SUGERENCIA u OTRO")
        String categoria,

        @NotNull(message = "componente es obligatorio")
        @Pattern(regexp = "JUEGO|PWA|OTRO", message = "componente debe ser JUEGO, PWA u OTRO")
        String componente,

        @NotBlank(message = "mensaje es obligatorio")
        @Size(min = 10, max = 1000, message = "mensaje debe tener entre 10 y 1000 caracteres")
        String mensaje) {
}
