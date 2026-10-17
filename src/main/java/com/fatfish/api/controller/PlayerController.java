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


    @GetMapping ("/newuser")
    @Operation(summary = "Crea un nuevo jugador",
            description = "Crea un nuevo jugador, si existe manda 0, si es nuevo 1.")
    public int createNewPlayer(@PathVariable int playerId) {
        return playerService.createNewPlayer(playerId);
    }

    
   
}
