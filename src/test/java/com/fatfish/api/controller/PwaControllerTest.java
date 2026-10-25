package com.fatfish.api.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fatfish.api.dto.pwa.FeedbackResponse;
import com.fatfish.api.dto.pwa.MatchDto;
import com.fatfish.api.dto.pwa.PlayerSummaryDto;
import com.fatfish.api.dto.pwa.ProfileDto;
import com.fatfish.api.dto.pwa.PwaPage;
import com.fatfish.api.dto.pwa.RankingDto;
import com.fatfish.api.dto.pwa.RankingEntryDto;
import com.fatfish.api.dto.pwa.SurveyResponse;
import com.fatfish.api.exception.PlayerNotFoundException;
import com.fatfish.api.service.PwaService;
import com.fatfish.api.service.PwaService.FeedbackResult;
import com.fatfish.api.service.PwaService.SurveyResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PwaController.class)
class PwaControllerTest {

    private static final long PLAYER_NUMBER = 1;
    private static final UUID KEY = UUID.randomUUID();
    private static final String VALID_FEEDBACK = """
            { "categoria": "SUGERENCIA", "componente": "PWA", "mensaje": "Agregar el ranking semanal" }
            """;
    private static final String VALID_SURVEY = """
            { "percepcionRiesgo": 4, "cambioOpinion": 3, "claridadMensaje": 5, "comentario": "Buen mensaje" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PwaService pwaService;

    @Test
    void returnsMatchHistory() throws Exception {
        MatchDto match = new MatchDto("r1", "DERROTA", 4, 281, 2350, 1800, 1450, 1301,
                Instant.parse("2026-10-05T18:20:11Z"));
        when(pwaService.getMatches(PLAYER_NUMBER, 0, 10)).thenReturn(
                new PwaPage<>(List.of(match), 0, 10, 1, 1, true, Instant.now()));

        mockMvc.perform(get("/api/pwa/partidas").param("id", String.valueOf(PLAYER_NUMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].resultado").value("DERROTA"))
                .andExpect(jsonPath("$.contenido[0].rondaAlcanzada").value(4))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.ultima").value(true))
                .andExpect(jsonPath("$.generadoEn").exists());
    }

    @Test
    void returnsNotFoundForUnknownPlayer() throws Exception {
        when(pwaService.getMatches(PLAYER_NUMBER, 0, 10)).thenThrow(new PlayerNotFoundException(PLAYER_NUMBER));

        mockMvc.perform(get("/api/pwa/partidas").param("id", String.valueOf(PLAYER_NUMBER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el jugador #1"));
    }

    @Test
    void returnsProfile() throws Exception {
        when(pwaService.getProfile(PLAYER_NUMBER)).thenReturn(new ProfileDto("1", "jesus",
                Instant.parse("2026-09-20T15:00:00Z"), Instant.parse("2026-10-13T22:44:05Z"),
                new ProfileDto.Statistics(8, 2, 31950, 13100, 3, 12855), Instant.now()));

        mockMvc.perform(get("/api/pwa/perfil").param("id", String.valueOf(PLAYER_NUMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("jesus"))
                .andExpect(jsonPath("$.estadisticas.rachaDerrotasMax").value(3));
    }

    @Test
    void rejectsNonNumericPlayerId() throws Exception {
        mockMvc.perform(get("/api/pwa/partidas").param("id", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pwaService);
    }

    @Test
    void rejectsPageSizeAboveFifty() throws Exception {
        mockMvc.perform(get("/api/pwa/partidas").param("id", String.valueOf(PLAYER_NUMBER)).param("tamano", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("tamano debe estar entre 1 y 50")));
        verifyNoInteractions(pwaService);
    }

    @Test
    void createsFeedback() throws Exception {
        when(pwaService.createFeedback(eq(KEY), any())).thenReturn(new FeedbackResult(
                new FeedbackResponse("7", Instant.parse("2026-10-14T20:00:00Z")), true));

        mockMvc.perform(post("/api/pwa/retroalimentacion")
                        .header("Idempotency-Key", KEY.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_FEEDBACK))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/pwa/retroalimentacion/7"))
                .andExpect(jsonPath("$.id").value("7"));
    }

    @Test
    void returnsExistingFeedbackForRepeatedKey() throws Exception {
        when(pwaService.createFeedback(eq(KEY), any())).thenReturn(new FeedbackResult(
                new FeedbackResponse("7", Instant.parse("2026-10-14T20:00:00Z")), false));

        mockMvc.perform(post("/api/pwa/retroalimentacion")
                        .header("Idempotency-Key", KEY.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_FEEDBACK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("7"));
    }

    @Test
    void rejectsInvalidFeedback() throws Exception {
        mockMvc.perform(post("/api/pwa/retroalimentacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "categoria": "QUEJA", "componente": "PWA", "mensaje": "corto" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("categoria debe ser ERROR, SUGERENCIA u OTRO")))
                .andExpect(jsonPath("$.errors", hasItem("mensaje debe tener entre 10 y 1000 caracteres")));
        verifyNoInteractions(pwaService);
    }

    @Test
    void createsSurvey() throws Exception {
        when(pwaService.createSurvey(eq(KEY), any())).thenReturn(new SurveyResult(
                new SurveyResponse("3", true, Instant.parse("2026-10-14T20:00:00Z")), true));

        mockMvc.perform(post("/api/pwa/encuesta")
                        .header("Idempotency-Key", KEY.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SURVEY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true));
    }

    @Test
    void returnsOkForRepeatedSurveyKey() throws Exception {
        when(pwaService.createSurvey(eq(KEY), any())).thenReturn(new SurveyResult(
                new SurveyResponse("3", false, Instant.parse("2026-10-14T20:00:00Z")), false));

        mockMvc.perform(post("/api/pwa/encuesta")
                        .header("Idempotency-Key", KEY.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_SURVEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    void rejectsInvalidSurvey() throws Exception {
        mockMvc.perform(post("/api/pwa/encuesta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "percepcionRiesgo": 6, "cambioOpinion": 3, "claridadMensaje": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("percepcionRiesgo debe estar entre 1 y 5")))
                .andExpect(jsonPath("$.errors", hasItem("claridadMensaje debe estar entre 1 y 5")));
        verifyNoInteractions(pwaService);
    }

    @Test
    void returnsRankingWithPlayerPosition() throws Exception {
        RankingEntryDto entry = new RankingEntryDto(1, "5", "jesus", 1510, Instant.parse("2026-10-13T22:00:00Z"));
        when(pwaService.getRanking(0, 10, 5L)).thenReturn(new RankingDto(
                new PwaPage<>(List.of(entry), 0, 10, 1, 1, true, Instant.now()), entry));

        mockMvc.perform(get("/api/pwa/ranking").param("pagina", "0").param("tamano", "10").param("jugadorId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina.contenido[0].alias").value("jesus"))
                .andExpect(jsonPath("$.pagina.contenido[0].puntaje").value(1510))
                .andExpect(jsonPath("$.pagina.totalElementos").value(1))
                .andExpect(jsonPath("$.miPosicion.posicion").value(1));
    }

    @Test
    void rejectsRankingPageBeyondTopFifty() throws Exception {
        mockMvc.perform(get("/api/pwa/ranking").param("pagina", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("pagina debe estar entre 0 y 49")));
        verifyNoInteractions(pwaService);
    }

    @Test
    void rejectsRankingPageSizeAboveTen() throws Exception {
        mockMvc.perform(get("/api/pwa/ranking").param("tamano", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("tamano debe estar entre 1 y 10")));
        verifyNoInteractions(pwaService);
    }

    @Test
    void searchesPlayers() throws Exception {
        when(pwaService.searchPlayers("andrik")).thenReturn(List.of(
                new PlayerSummaryDto("7", "andrik", Instant.parse("2026-10-13T22:44:05Z"))));

        mockMvc.perform(get("/api/pwa/jugadores").param("busqueda", "andrik"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jugadorId").value("7"))
                .andExpect(jsonPath("$[0].alias").value("andrik"));
    }

    @Test
    void returnsEmptyListWhenNoPlayerMatches() throws Exception {
        when(pwaService.searchPlayers("zzz")).thenReturn(List.of());

        mockMvc.perform(get("/api/pwa/jugadores").param("busqueda", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsBlankPlayerSearch() throws Exception {
        mockMvc.perform(get("/api/pwa/jugadores").param("busqueda", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("busqueda es obligatoria")));
        verifyNoInteractions(pwaService);
    }
}
