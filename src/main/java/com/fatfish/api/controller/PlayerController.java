package com.fatfish.api.controller;

import com.fatfish.api.dto.PageResponse;
import com.fatfish.api.dto.PlayerStatsResponse;
import com.fatfish.api.dto.RunRecord;
import com.fatfish.api.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players/{playerId}")
@Tag(name = "Jugadores", description = "Historial y estadísticas por jugador")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/runs")
    @Operation(summary = "Historial paginado de partidas",
            description = "Ordenado de la más reciente a la más antigua. La página empieza en 0.")
    public PageResponse<RunRecord> getRuns(
            @PathVariable UUID playerId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page no puede ser negativo") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size debe estar entre 1 y 100")
            @Max(value = 100, message = "size debe estar entre 1 y 100") int size) {
        return playerService.getRuns(playerId, page, size);
    }

    @GetMapping("/stats")
    @Operation(summary = "Estadísticas agregadas del jugador")
    public PlayerStatsResponse getStats(@PathVariable UUID playerId) {
        return playerService.getStats(playerId);
    }
}
