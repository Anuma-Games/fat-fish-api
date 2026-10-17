package com.fatfish.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** PWA endpoints against a real PostgreSQL (profile "test"). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PwaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE retroalimentacion, partida_comodin, partida, estadistica_jugador, jugador RESTART IDENTITY");
    }

    @Test
    void storesFeedbackOnlyOnceForTheSameIdempotencyKey() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = """
                { "categoria": "ERROR", "componente": "JUEGO", "mensaje": "La bola se atoró en el 17" }
                """;

        mockMvc.perform(post("/api/pwa/retroalimentacion").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"));
        mockMvc.perform(post("/api/pwa/retroalimentacion").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM retroalimentacion", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT mensaje FROM retroalimentacion", String.class))
                .isEqualTo("La bola se atoró en el 17");
    }

    @Test
    void returnsTheProfileAndHistoryOfAPlayer() throws Exception {
        jdbc.update("INSERT INTO jugador (alias, token_hash, ultima_actividad) VALUES ('jesus', 'hash-1', '2026-10-02 10:00Z')");
        jdbc.update("""
                INSERT INTO estadistica_jugador (jugador_id, puntaje_maximo, puntaje_maximo_en, partidas_totales,
                    partidas_ganadas, dinero_invertido, dinero_ganado, dinero_perdido, racha_derrotas_max,
                    tiempo_jugado_seg)
                VALUES (1, 1510, '2026-10-02 10:00Z', 2, 1, 11450, 13000, 3350, 1, 3000)
                """);
        jdbc.update("""
                INSERT INTO partida (jugador_id, numero_local, resultado, ronda_alcanzada, puntaje, dinero_invertido,
                    dinero_ganado, dinero_perdido, veces_todo_o_nada, empenos, prestamos, duracion_seg, creada_en)
                VALUES
                (1, 1, 'DERROTA', 4, 281, 2350, 1800, 1450, 0, 0, 0, 1200, '2026-10-01 10:00Z'),
                (1, 2, 'VICTORIA', 9, 1510, 9100, 11200, 1900, 1, 0, 0, 1800, '2026-10-02 10:00Z')
                """);

        mockMvc.perform(get("/api/pwa/partidas").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[0].id").value("2"))
                .andExpect(jsonPath("$.contenido[0].resultado").value("VICTORIA"))
                .andExpect(jsonPath("$.contenido[1].resultado").value("DERROTA"));

        mockMvc.perform(get("/api/pwa/perfil").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadorId").value("1"))
                .andExpect(jsonPath("$.alias").value("jesus"))
                .andExpect(jsonPath("$.estadisticas.partidasTotales").value(2))
                .andExpect(jsonPath("$.estadisticas.partidasGanadas").value(1))
                .andExpect(jsonPath("$.estadisticas.totalInvertido").value(11450))
                .andExpect(jsonPath("$.estadisticas.totalPerdido").value(3350))
                .andExpect(jsonPath("$.estadisticas.rachaDerrotasMax").value(1))
                .andExpect(jsonPath("$.estadisticas.tiempoJugadoSeg").value(3000))
                .andExpect(jsonPath("$.ultimaActividad").value("2026-10-02T10:00:00Z"));

        mockMvc.perform(get("/api/pwa/perfil").param("id", "99")).andExpect(status().isNotFound());
    }
}
