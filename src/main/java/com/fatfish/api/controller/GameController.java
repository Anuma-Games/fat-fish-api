package com.fatfish.api.controller;

import com.fatfish.api.dto.juego.MatchRequest;
import com.fatfish.api.dto.juego.MatchResponse;
import com.fatfish.api.dto.juego.RegisterRequest;
import com.fatfish.api.dto.juego.SessionResponse;
import com.fatfish.api.service.GameService;
import com.fatfish.api.service.GameService.MatchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/juego")
@Tag(name = "Juego", description = "Registro del jugador y envío de partidas desde el juego")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar jugador",
            description = "Crea el jugador y responde con su id y su token. El token solo se entrega aquí: "
                    + "el juego debe guardarlo y enviarlo en la cabecera Authorization.")
    public SessionResponse register(@Valid @RequestBody RegisterRequest request) {
        return gameService.register(request);
    }

    @PostMapping("/partidas")
    @Operation(summary = "Registrar partida",
            description = "Requiere Authorization: Bearer <token>. Responde 201 al guardarla y actualiza las "
                    + "estadísticas del jugador. Si el numeroLocal ya existe para el jugador, responde 200 con la "
                    + "partida guardada y no inserta un duplicado.")
    public ResponseEntity<MatchResponse> recordMatch(
            @Parameter(description = "Bearer <token>")
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody MatchRequest request) {
        MatchResult result = gameService.recordMatch(authorization, request);
        if (!result.created()) {
            return ResponseEntity.ok(result.match());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/juego/partidas/" + result.match().id()))
                .body(result.match());
    }
}
