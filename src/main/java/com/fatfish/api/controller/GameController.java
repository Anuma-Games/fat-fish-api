package com.fatfish.api.controller;

import com.fatfish.api.dto.juego.ChangePasswordRequest;
import com.fatfish.api.dto.juego.LoginRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/juego")
@Tag(name = "Juego", description = "Registro, sesión y contraseña del jugador, y envío de partidas desde el juego")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar jugador",
            description = "Crea el jugador con alias y contraseña y responde con su id y su token. El juego "
                    + "debe guardar el token y enviarlo en la cabecera Authorization.")
    public SessionResponse register(@Valid @RequestBody RegisterRequest request) {
        return gameService.register(request);
    }

    @PostMapping("/sesion")
    @Operation(summary = "Iniciar sesión",
            description = "Con el id del jugador y su contraseña entrega un token nuevo, por ejemplo en otro "
                    + "dispositivo. El token anterior deja de funcionar.")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        return gameService.login(request);
    }

    @PutMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar contraseña",
            description = "Requiere Authorization: Bearer <token>. No pide la contraseña anterior: sirve para "
                    + "recuperar la cuenta desde el dispositivo que tiene el token.")
    public void changePassword(
            @Parameter(description = "Bearer <token>")
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ChangePasswordRequest request) {
        gameService.changePassword(authorization, request);
    }

    @PostMapping("/partidas")
    @Operation(summary = "Registrar partida",
            description = "Requiere Authorization: Bearer <token>. Responde 201 al guardarla y actualiza las "
                    + "estadísticas del jugador. Si la claveIdempotencia ya existe, responde 200 con la partida "
                    + "guardada y no inserta un duplicado.")
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
