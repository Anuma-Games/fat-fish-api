package com.fatfish.api.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fatfish.api.dto.juego.MatchResponse;
import com.fatfish.api.dto.juego.SessionResponse;
import com.fatfish.api.exception.InvalidCredentialsException;
import com.fatfish.api.exception.InvalidTokenException;
import com.fatfish.api.service.GameService;
import com.fatfish.api.service.GameService.MatchResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameController.class)
class GameControllerTest {

    private static final String AUTHORIZATION = "Bearer abc";
    private static final String VALID_MATCH = """
            { "claveIdempotencia": "3f2c9a1e-7b4d-4c8a-9e21-5d0f6a8b7c13", "terminadaEn": "2026-10-16T17:40:00Z",
              "resultado": "DERROTA", "rondaAlcanzada": 4, "puntaje": 281,
              "dineroInvertido": 2350, "dineroGanado": 1800, "dineroPerdido": 1450, "vecesTodoONada": 1,
              "empenos": 0, "prestamos": 1, "duracionSeg": 1301 }
            """;
    private static final MatchResponse STORED = new MatchResponse("12",
            UUID.fromString("3f2c9a1e-7b4d-4c8a-9e21-5d0f6a8b7c13"), Instant.parse("2026-10-16T18:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @Test
    void registersAPlayer() throws Exception {
        when(gameService.register(any())).thenReturn(new SessionResponse("5", "token-del-juego"));

        mockMvc.perform(post("/api/juego/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"alias\": \"andrik\", \"contrasena\": \"pez-gordo-2026\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jugadorId").value("5"))
                .andExpect(jsonPath("$.token").value("token-del-juego"));
    }

    @Test
    void rejectsAShortAliasAndPassword() throws Exception {
        mockMvc.perform(post("/api/juego/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"alias\": \"ab\", \"contrasena\": \"1234\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("alias debe tener entre 3 y 24 caracteres")))
                .andExpect(jsonPath("$.errors", hasItem("contrasena debe tener entre 8 y 72 caracteres")));
        verifyNoInteractions(gameService);
    }

    @Test
    void returnsUnauthorizedForWrongCredentials() throws Exception {
        when(gameService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/juego/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"jugadorId\": 5, \"contrasena\": \"equivocada\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("El id del jugador o la contraseña no son correctos"));
    }

    @Test
    void changesThePassword() throws Exception {
        mockMvc.perform(put("/api/juego/contrasena").header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"contrasenaNueva\": \"nueva-clave-123\" }"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createsAMatch() throws Exception {
        when(gameService.recordMatch(eq(AUTHORIZATION), any())).thenReturn(new MatchResult(STORED, true));

        mockMvc.perform(post("/api/juego/partidas").header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_MATCH))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/juego/partidas/12"))
                .andExpect(jsonPath("$.claveIdempotencia").value("3f2c9a1e-7b4d-4c8a-9e21-5d0f6a8b7c13"));
    }
    @Test
    void acceptsAMatchBeyondRoundNine() throws Exception {
        when(gameService.recordMatch(eq(AUTHORIZATION), any())).thenReturn(new MatchResult(STORED, true));

        mockMvc.perform(post("/api/juego/partidas").header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_MATCH.replace("\"rondaAlcanzada\": 4", "\"rondaAlcanzada\": 12")))
                .andExpect(status().isCreated());
    }
    @Test
    void returnsTheStoredMatchForARepeatedKey() throws Exception {
        when(gameService.recordMatch(eq(AUTHORIZATION), any())).thenReturn(new MatchResult(STORED, false));

        mockMvc.perform(post("/api/juego/partidas").header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_MATCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("12"));
    }

    @Test
    void returnsUnauthorizedForAnInvalidToken() throws Exception {
        when(gameService.recordMatch(any(), any())).thenThrow(new InvalidTokenException());

        mockMvc.perform(post("/api/juego/partidas").contentType(MediaType.APPLICATION_JSON).content(VALID_MATCH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void rejectsAnInvalidMatch() throws Exception {
        mockMvc.perform(post("/api/juego/partidas").header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "resultado": "EMPATE", "rondaAlcanzada": 10, "puntaje": -1,
                                  "dineroInvertido": 0, "dineroGanado": 0, "dineroPerdido": 0, "vecesTodoONada": 0,
                                  "empenos": 0, "prestamos": 0, "duracionSeg": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("claveIdempotencia es obligatoria")))
                .andExpect(jsonPath("$.errors", hasItem("resultado debe ser VICTORIA, DERROTA o ABANDONO")))
                .andExpect(jsonPath("$.errors", hasItem("rondaAlcanzada debe estar entre 1 y 9")));
        verifyNoInteractions(gameService);
    }
}
