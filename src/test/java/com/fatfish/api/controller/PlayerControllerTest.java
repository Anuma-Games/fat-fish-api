package com.fatfish.api.controller;

import static com.fatfish.api.RunRecordBuilder.aRun;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fatfish.api.dto.PageResponse;
import com.fatfish.api.dto.PlayerStatsResponse;
import com.fatfish.api.dto.RunRecord;
import com.fatfish.api.exception.PlayerNotFoundException;
import com.fatfish.api.service.PlayerService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @Test
    void returnsStats() throws Exception {
        when(playerService.getStats(PLAYER_ID))
                .thenReturn(new PlayerStatsResponse(PLAYER_ID, 3, 2, 144, 5000, 4200, 2900));

        mockMvc.perform(get("/api/v1/players/{id}/stats", PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(PLAYER_ID.toString()))
                .andExpect(jsonPath("$.runsPlayed").value(3))
                .andExpect(jsonPath("$.maxLevelReached").value(2))
                .andExpect(jsonPath("$.maxSpinScore").value(144))
                .andExpect(jsonPath("$.totalBet").value(5000))
                .andExpect(jsonPath("$.totalWon").value(4200))
                .andExpect(jsonPath("$.totalLost").value(2900));
    }

    @Test
    void returnsNotFoundForUnknownPlayer() throws Exception {
        when(playerService.getStats(PLAYER_ID)).thenThrow(new PlayerNotFoundException(PLAYER_ID));

        mockMvc.perform(get("/api/v1/players/{id}/stats", PLAYER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Jugador no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el jugador " + PLAYER_ID));
    }

    @Test
    void returnsPagedHistoryWithDefaults() throws Exception {
        RunRecord record = aRun().playerId(PLAYER_ID).build();
        when(playerService.getRuns(PLAYER_ID, 0, 20)).thenReturn(new PageResponse<>(List.of(record), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/players/{id}/runs", PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].runId").value(record.runId().toString()))
                .andExpect(jsonPath("$.content[0].startedAt").value("2026-10-05T18:20:11Z"))
                .andExpect(jsonPath("$.content[0].pawnedAssets[0]").value("car"))
                .andExpect(jsonPath("$.content[0].mostDesperateSpin.allIn").value(true))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/players/{id}/runs", PLAYER_ID).param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("size debe estar entre 1 y 100")));
        verifyNoInteractions(playerService);
    }

    @Test
    void rejectsMalformedPlayerId() throws Exception {
        mockMvc.perform(get("/api/v1/players/{id}/stats", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El parámetro 'playerId' tiene un formato inválido"));
        verifyNoInteractions(playerService);
    }
}
