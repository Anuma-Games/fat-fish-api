package com.fatfish.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record DesperateSpinDto(
        @Min(value = 1, message = "mostDesperateSpin.level debe estar entre 1 y 3")
        @Max(value = 3, message = "mostDesperateSpin.level debe estar entre 1 y 3")
        Integer level,
        @PositiveOrZero(message = "mostDesperateSpin.round no puede ser negativo")
        Integer round,
        @PositiveOrZero(message = "mostDesperateSpin.spin no puede ser negativo")
        Integer spin,
        @PositiveOrZero(message = "mostDesperateSpin.bet no puede ser negativo")
        Long bet,
        @PositiveOrZero(message = "mostDesperateSpin.moneyBefore no puede ser negativo")
        Long moneyBefore,
        Boolean allIn) {
}
