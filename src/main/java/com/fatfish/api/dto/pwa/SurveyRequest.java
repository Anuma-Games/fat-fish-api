package com.fatfish.api.dto.pwa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SurveyRequest(
        @NotNull(message = "percepcionRiesgo es obligatoria")
        @Min(value = 1, message = "percepcionRiesgo debe estar entre 1 y 5")
        @Max(value = 5, message = "percepcionRiesgo debe estar entre 1 y 5")
        Integer percepcionRiesgo,

        @NotNull(message = "cambioOpinion es obligatorio")
        @Min(value = 1, message = "cambioOpinion debe estar entre 1 y 5")
        @Max(value = 5, message = "cambioOpinion debe estar entre 1 y 5")
        Integer cambioOpinion,

        @NotNull(message = "claridadMensaje es obligatoria")
        @Min(value = 1, message = "claridadMensaje debe estar entre 1 y 5")
        @Max(value = 5, message = "claridadMensaje debe estar entre 1 y 5")
        Integer claridadMensaje,

        @Size(max = 500, message = "comentario no puede superar los 500 caracteres")
        String comentario) {
}