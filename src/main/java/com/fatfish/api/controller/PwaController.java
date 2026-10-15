package com.fatfish.api.controller;

import com.fatfish.api.dto.pwa.FeedbackRequest;
import com.fatfish.api.dto.pwa.FeedbackResponse;
import com.fatfish.api.dto.pwa.MatchDto;
import com.fatfish.api.dto.pwa.ProfileDto;
import com.fatfish.api.dto.pwa.PwaPage;
import com.fatfish.api.service.PwaService;
import com.fatfish.api.service.PwaService.FeedbackResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
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

    @GetMapping("/perfil")
    @Operation(summary = "Perfil del jugador",
            description = "Alias, fechas y las seis estadísticas del expediente, calculadas con todas sus partidas.")
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
}
