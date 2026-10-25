package com.fatfish.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fatfish.api.dto.pwa.FeedbackRequest;
import com.fatfish.api.dto.pwa.SurveyRequest;
import com.fatfish.api.service.PwaService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    @Autowired
    private PwaService pwaService;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE encuesta, retroalimentacion, partida_comodin, partida, estadistica_jugador, jugador RESTART IDENTITY");
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
        jdbc.update("INSERT INTO jugador (alias, contrasena_hash, token_hash, ultima_actividad) VALUES ('jesus', 'bcrypt-1', 'hash-1', '2026-10-02 10:00Z')");
        jdbc.update("""
                INSERT INTO estadistica_jugador (jugador_id, puntaje_maximo, puntaje_maximo_en, partidas_totales,
                    partidas_ganadas, dinero_invertido, dinero_ganado, dinero_perdido, racha_derrotas_max,
                    tiempo_jugado_seg)
                VALUES (1, 1510, '2026-10-02 10:00Z', 2, 1, 11450, 13000, 3350, 1, 3000)
                """);
        jdbc.update("""
                INSERT INTO partida (jugador_id, clave_idempotencia, terminada_en, resultado, ronda_alcanzada, puntaje, dinero_invertido,
                    dinero_ganado, dinero_perdido, veces_todo_o_nada, empenos, prestamos, duracion_seg, creada_en)
                VALUES
                (1, gen_random_uuid(), '2026-10-01 10:00Z', 'DERROTA', 4, 281, 2350, 1800, 1450, 0, 0, 0, 1200, '2026-10-01 10:05Z'),
                (1, gen_random_uuid(), '2026-10-02 10:00Z', 'VICTORIA', 9, 1510, 9100, 11200, 1900, 1, 0, 0, 1800, '2026-10-02 10:05Z')
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

    @Test
    void storesSurveyOnlyOnceForTheSameIdempotencyKey() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = """
                { "percepcionRiesgo": 4, "cambioOpinion": 3, "claridadMensaje": 5, "comentario": "   " }
                """;

        mockMvc.perform(post("/api/pwa/encuesta").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(true));
        mockMvc.perform(post("/api/pwa/encuesta").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.created").value(false));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM encuesta", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT comentario FROM encuesta", String.class)).isNull();
    }

    @Test
    void searchesPlayersByIdAndAlias() throws Exception {
        jdbc.update("""
                INSERT INTO jugador (alias, contrasena_hash, token_hash, ultima_actividad) VALUES
                ('Andrik', 'bcrypt-1', 'hash-1', '2026-10-02 10:00Z'),
                ('andrik2', 'bcrypt-2', 'hash-2', '2026-10-02 12:00Z'),
                ('gael', 'bcrypt-3', 'hash-3', '2026-10-02 11:00Z'),
                ('ANDRIKO', 'bcrypt-4', 'hash-4', NULL)
                """);

        // Ignores case and orders by last activity, players without activity last
        mockMvc.perform(get("/api/pwa/jugadores").param("busqueda", "andrik"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].jugadorId").value("2"))
                .andExpect(jsonPath("$[1].jugadorId").value("1"))
                .andExpect(jsonPath("$[2].jugadorId").value("4"));

        // Player 2 matches by id and by alias, but appears once
        mockMvc.perform(get("/api/pwa/jugadores").param("busqueda", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].alias").value("andrik2"));

        // % is plain text, not a LIKE wildcard
        mockMvc.perform(get("/api/pwa/jugadores").param("busqueda", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void ranksTheTopFiftyAndThePositionOfAnyPlayer() throws Exception {
        // 55 players with victories (player i scores 2000 - 10 * i) and player 56 without victories
        jdbc.update("""
                INSERT INTO jugador (alias, contrasena_hash, token_hash)
                SELECT 'pez' || i, 'bcrypt-' || i, 'hash-' || i FROM generate_series(1, 56) AS i
                """);
        jdbc.update("""
                INSERT INTO estadistica_jugador (jugador_id, puntaje_maximo, puntaje_maximo_en)
                SELECT i, 2000 - 10 * i, '2026-10-01 00:00Z'::timestamptz + i * interval '1 minute'
                FROM generate_series(1, 55) AS i
                """);
        jdbc.update("INSERT INTO estadistica_jugador (jugador_id) VALUES (56)");
        // Player 2 ties player 1, but reached the score first
        jdbc.update("UPDATE estadistica_jugador SET puntaje_maximo = 1990, puntaje_maximo_en = '2026-10-01 00:00Z' WHERE jugador_id = 2");

        mockMvc.perform(get("/api/pwa/ranking").param("pagina", "0").param("tamano", "10").param("jugadorId", "55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina.contenido", hasSize(10)))
                .andExpect(jsonPath("$.pagina.contenido[0].jugadorId").value("2"))
                .andExpect(jsonPath("$.pagina.contenido[1].jugadorId").value("1"))
                .andExpect(jsonPath("$.pagina.contenido[1].posicion").value(2))
                .andExpect(jsonPath("$.pagina.totalElementos").value(50))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(5))
                .andExpect(jsonPath("$.miPosicion.posicion").value(55))
                .andExpect(jsonPath("$.miPosicion.puntaje").value(1450));

        // With pages of 7 the last page only has position 50
        mockMvc.perform(get("/api/pwa/ranking").param("pagina", "7").param("tamano", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina.contenido", hasSize(1)))
                .andExpect(jsonPath("$.pagina.contenido[0].posicion").value(50))
                .andExpect(jsonPath("$.pagina.totalPaginas").value(8))
                .andExpect(jsonPath("$.pagina.ultima").value(true));

        mockMvc.perform(get("/api/pwa/ranking").param("pagina", "5").param("tamano", "10").param("jugadorId", "56"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina.contenido", hasSize(0)))
                .andExpect(jsonPath("$.miPosicion").value(nullValue()));
    }

    @Test
    void concurrentRetriesWithTheSameKeyStoreOneSurveyAndOneFeedback() throws Exception {
        SurveyRequest survey = new SurveyRequest(4, 3, 5, null);
        FeedbackRequest feedback = new FeedbackRequest("ERROR", "PWA", "El ranking no carga en el celular");
        int rounds = 20;

        for (int i = 0; i < rounds; i++) {
            UUID surveyKey = UUID.randomUUID();
            UUID feedbackKey = UUID.randomUUID();
            assertThat(runTwiceAtOnce(() -> pwaService.createSurvey(surveyKey, survey).created()))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(runTwiceAtOnce(() -> pwaService.createFeedback(feedbackKey, feedback).created()))
                    .containsExactlyInAnyOrder(true, false);
        }

        assertThat(jdbc.queryForObject("SELECT count(*) FROM encuesta", Integer.class)).isEqualTo(rounds);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM retroalimentacion", Integer.class)).isEqualTo(rounds);
    }

    /** Runs the same call from two threads released at the same time, like two overlapping retries of the PWA. */
    private static List<Boolean> runTwiceAtOnce(Callable<Boolean> call) throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Boolean> task = () -> {
            start.await();
            return call.call();
        };
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(task);
            Future<Boolean> second = executor.submit(task);
            return List.of(first.get(), second.get());
        }
    }
}
