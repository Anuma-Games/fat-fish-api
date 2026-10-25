package com.fatfish.api.controller;

import com.fatfish.api.dto.pwa.FeedbackRequest;
import com.fatfish.api.dto.pwa.FeedbackResponse;
import com.fatfish.api.dto.pwa.MatchDto;
import com.fatfish.api.dto.pwa.PlayerSummaryDto;
import com.fatfish.api.dto.pwa.ProfileDto;
import com.fatfish.api.dto.pwa.PwaPage;
import com.fatfish.api.dto.pwa.RankingDto;
import com.fatfish.api.dto.pwa.SurveyRequest;
import com.fatfish.api.dto.pwa.SurveyResponse;
import com.fatfish.api.service.PwaService;
import com.fatfish.api.service.PwaService.FeedbackResult;
import com.fatfish.api.service.PwaService.SurveyResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pwa")
@Tag(name = "PWA", description = "Consultas e inserciones que usa la PWA")
public class PwaController {

    private final PwaService pwaService;

    public PwaController(PwaService pwaService) {
        this.pwaService = pwaService;
    }

    @GetMapping("/jugadores")
    @Operation(summary = "Buscar jugadores",
            description = "Busca por id exacto (solo dígitos) y por alias que contenga el texto, sin distinguir "
                    + "mayúsculas. La coincidencia por id va primero; después, por última actividad. "
                    + "Máximo 20 resultados; sin coincidencias responde 200 con una lista vacía.")
    public List<PlayerSummaryDto> searchPlayers(
            @RequestParam
            @NotBlank(message = "busqueda es obligatoria")
            @Size(max = 24, message = "busqueda no puede superar los 24 caracteres") String busqueda) {
        return pwaService.searchPlayers(busqueda);
    }

    @GetMapping("/perfil")
    @Operation(summary = "Perfil del jugador",
            description = "Alias, fechas y las seis estadísticas del expediente, que se actualizan con cada partida.")
    public ProfileDto getProfile(
            @RequestParam @Min(value = 1, message = "id debe ser un número de jugador válido") long id) {
        return pwaService.getProfile(id);
    }

    @GetMapping("/partidas")
    @Operation(summary = "Historial de partidas del jugador",
            description = "De la más reciente a la más antigua. La página empieza en 0.")
    public PwaPage<MatchDto> getMatches(
            @RequestParam @Min(value = 1, message = "id debe ser un número de jugador válido") long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "pagina no puede ser negativa") int pagina,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "tamano debe estar entre 1 y 50")
            @Max(value = 50, message = "tamano debe estar entre 1 y 50") int tamano) {
        return pwaService.getMatches(id, pagina, tamano);
    }

    @GetMapping("/ranking")
    @Operation(summary = "Ranking mundial",
            description = "Mejor puntaje en una victoria de cada jugador, hasta el lugar 50. La página empieza en 0 "
                    + "y el número de páginas depende de tamano (5 páginas de 10). Si se envía jugadorId, miPosicion "
                    + "trae su lugar real; es nulo si no se envió o no tiene victorias.")
    public RankingDto getRanking(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "pagina debe estar entre 0 y 49")
            @Max(value = 49, message = "pagina debe estar entre 0 y 49") int pagina,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "tamano debe estar entre 1 y 10")
            @Max(value = 10, message = "tamano debe estar entre 1 y 10") int tamano,
            @RequestParam(required = false)
            @Min(value = 1, message = "jugadorId debe ser un número de jugador válido") Long jugadorId) {
        return pwaService.getRanking(pagina, tamano, jugadorId);
    }

    @PostMapping("/retroalimentacion")
    @Operation(summary = "Enviar retroalimentación",
            description = "Responde 201 al crearla. Si la cabecera Idempotency-Key ya se usó, "
                    + "responde 200 con el registro existente y no inserta un duplicado.")
    public ResponseEntity<FeedbackResponse> createFeedback(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody FeedbackRequest request) {
        UUID key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID();
        FeedbackResult result = pwaService.createFeedback(key, request);
        if (!result.created()) {
            return ResponseEntity.ok(result.feedback());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/pwa/retroalimentacion/" + result.feedback().id()))
                .body(result.feedback());
    }

    @PostMapping("/encuesta")
    @Operation(summary = "Enviar encuesta anónima",
            description = "Responde 201 con el id y la fecha de recepción; no guarda ningún dato del jugador. "
                    + "Si la cabecera Idempotency-Key ya se usó, responde 200 con created en false y no inserta "
                    + "un duplicado.")
    public ResponseEntity<SurveyResponse> createSurvey(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody SurveyRequest request) {
        UUID key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID();
        SurveyResult result = pwaService.createSurvey(key, request);
        if (!result.created()) {
            return ResponseEntity.ok(result.survey());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.survey());
    }
}
