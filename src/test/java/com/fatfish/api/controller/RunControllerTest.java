package com.fatfish.api.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fatfish.api.dto.RejectedRun;
import com.fatfish.api.dto.RunBatchResponse;
import com.fatfish.api.service.RunService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunController.class)
class RunControllerTest {

    private static final String RUN_ID = "7d3f2a9e-1c4b-4f6e-9a8d-2b5c6e7f8a90";

    private static final String BATCH = """
            { "runs": [ {
                "schemaVersion": 1, "runId": "%s",
                "installationId": "0e8f7c6d-5b4a-4c3d-8e2f-1a0b9c8d7e6f", "playerId": null,
                "gameVersion": "0.3.0", "startedAt": "2026-10-05T18:20:11Z", "endedAt": "2026-10-05T18:41:52Z",
                "result": "defeat", "levelReached": 2, "roundsCleared": 4, "bossesDefeated": 1,
                "maxSpinScore": 144, "totalSpins": 17, "totalBet": 2350, "totalWon": 1800, "totalLost": 1450,
                "allInCount": 1, "pawnedAssets": ["car"], "loansTaken": 1, "loanTotal": 500,
                "mostDesperateSpin": { "level": 2, "round": 3, "spin": 4, "bet": 900, "moneyBefore": 900, "allIn": true },
                "itemsBought": ["fresh_paint", "glass_chip"]
            } ] }
            """.formatted(RUN_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RunService runService;

    @Test
    void returnsBatchOutcome() throws Exception {
        UUID rejectedId = UUID.randomUUID();
        when(runService.ingest(anyList())).thenReturn(new RunBatchResponse(
                List.of(UUID.fromString(RUN_ID)),
                List.of(),
                List.of(new RejectedRun(1, rejectedId, List.of("levelReached debe estar entre 1 y 3")))));

        mockMvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content(BATCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted[0]").value(RUN_ID))
                .andExpect(jsonPath("$.duplicates").isEmpty())
                .andExpect(jsonPath("$.rejected[0].index").value(1))
                .andExpect(jsonPath("$.rejected[0].runId").value(rejectedId.toString()))
                .andExpect(jsonPath("$.rejected[0].reasons[0]").value("levelReached debe estar entre 1 y 3"));
    }

    @Test
    void rejectsEmptyBatch() throws Exception {
        mockMvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content("{ \"runs\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errors", hasItem("El lote debe contener al menos una partida")));
        verifyNoInteractions(runService);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content("{ \"runs\": [ "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "El cuerpo de la solicitud no es un JSON válido o tiene campos con formato incorrecto"));
        verifyNoInteractions(runService);
    }
}
