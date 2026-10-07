package com.fatfish.api.controller;

import com.fatfish.api.dto.RunBatchRequest;
import com.fatfish.api.dto.RunBatchResponse;
import com.fatfish.api.service.RunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runs")
@Tag(name = "Partidas", description = "Sincronización de partidas desde el juego")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping
    @Operation(summary = "Sincroniza un lote de partidas",
            description = "Idempotente por runId: reenviar un lote no duplica partidas. "
                    + "Responde las partidas aceptadas, duplicadas y rechazadas (con motivo).")
    public RunBatchResponse uploadBatch(@Valid @RequestBody RunBatchRequest request) {
        return runService.ingest(request.runs());
    }
}
