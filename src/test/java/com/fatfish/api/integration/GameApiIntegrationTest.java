package com.fatfish.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Game endpoints against a real PostgreSQL (profile "test"). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameApiIntegrationTest {

    private static final String PASSWORD = "pez-gordo-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE partida_comodin, partida, estadistica_jugador, jugador RESTART IDENTITY");
    }

    @Test
    void registersAPlayerAndStoresOnlyTheHashesOfItsPasswordAndToken() throws Exception {
        String token = register("andrik");

        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        Map<String, Object> player = jdbc.queryForMap(
                "SELECT alias, contrasena_hash, token_hash FROM jugador WHERE id = 1");
        assertThat(player.get("alias")).isEqualTo("andrik");
        assertThat((String) player.get("contrasena_hash")).startsWith("$2").doesNotContain(PASSWORD);
        assertThat((String) player.get("token_hash")).hasSize(64).isNotEqualTo(token);
        assertThat(jdbc.queryForObject("SELECT partidas_totales FROM estadistica_jugador WHERE jugador_id = 1",
                Integer.class)).isZero();
    }

    @Test
    void storesAMatchOnceAndUpdatesTheStatistics() throws Exception {
        String token = register("andrik");

        mockMvc.perform(post("/api/juego/partidas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(match(1, "VICTORIA", 1510, """
                                [ { "comodinId": 1, "vecesComprado": 2, "vecesUsado": 5 },
                                  { "comodinId": 3, "vecesComprado": 1, "vecesUsado": 0 } ]""")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/juego/partidas/1"))
                .andExpect(jsonPath("$.numeroLocal").value(1));
        mockMvc.perform(post("/api/juego/partidas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(match(1, "VICTORIA", 1510, "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM partida", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT sum(veces_usado) FROM partida_comodin", Integer.class)).isEqualTo(5);
        mockMvc.perform(get("/api/pwa/perfil").param("id", "1"))
                .andExpect(jsonPath("$.estadisticas.partidasTotales").value(1))
                .andExpect(jsonPath("$.estadisticas.partidasGanadas").value(1))
                .andExpect(jsonPath("$.ultimaActividad").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT puntaje_maximo FROM estadistica_jugador", Integer.class))
                .isEqualTo(1510);
    }

    @Test
    void countsTheLosingStreakInTheOrderOfTheLocalNumber() throws Exception {
        String token = register("andrik");

        // Match 2 arrives last (it was played offline): the streak is 1, 2, 3 by local number
        for (int localNumber : new int[] {1, 3, 2}) {
            mockMvc.perform(post("/api/juego/partidas").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(match(localNumber, "DERROTA", 100, null)))
                    .andExpect(status().isCreated());
        }

        assertThat(jdbc.queryForObject("SELECT racha_derrotas_max FROM estadistica_jugador", Integer.class))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT puntaje_maximo_en FROM estadistica_jugador", Object.class)).isNull();
    }

    @Test
    void rejectsAnInvalidTokenAndAnUnknownJoker() throws Exception {
        String token = register("andrik");

        mockMvc.perform(post("/api/juego/partidas").header("Authorization", "Bearer otro-token")
                        .contentType(MediaType.APPLICATION_JSON).content(match(1, "DERROTA", 0, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mockMvc.perform(post("/api/juego/partidas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(match(1, "DERROTA", 0, "[ { \"comodinId\": 9, \"vecesComprado\": 1, \"vecesUsado\": 1 } ]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La partida incluye un comodín que no existe"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM partida", Integer.class)).isZero();
    }

    @Test
    void logsInWithANewTokenAndChangesThePasswordWithTheToken() throws Exception {
        String firstToken = register("andrik");

        mockMvc.perform(post("/api/juego/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(login(1, "otra-contrasena")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("El id del jugador o la contraseña no son correctos"));
        mockMvc.perform(post("/api/juego/sesion").contentType(MediaType.APPLICATION_JSON).content(login(99, PASSWORD)))
                .andExpect(status().isUnauthorized());
        String body = mockMvc.perform(post("/api/juego/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(login(1, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadorId").value("1"))
                .andReturn().getResponse().getContentAsString();
        String secondToken = JsonPath.read(body, "$.token");

        // The previous token stops working
        mockMvc.perform(put("/api/juego/contrasena").header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"contrasenaNueva\": \"nueva-clave-123\" }"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/juego/contrasena").header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"contrasenaNueva\": \"nueva-clave-123\" }"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/juego/sesion").contentType(MediaType.APPLICATION_JSON).content(login(1, PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/juego/sesion").contentType(MediaType.APPLICATION_JSON)
                        .content(login(1, "nueva-clave-123")))
                .andExpect(status().isOk());
    }

    private String register(String alias) throws Exception {
        String body = mockMvc.perform(post("/api/juego/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"alias\": \"" + alias + "\", \"contrasena\": \"" + PASSWORD + "\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jugadorId").value("1"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String login(long playerId, String password) {
        return "{ \"jugadorId\": " + playerId + ", \"contrasena\": \"" + password + "\" }";
    }

    private static String match(int localNumber, String result, int score, String jokers) {
        return """
                { "numeroLocal": %d, "resultado": "%s", "rondaAlcanzada": 5, "puntaje": %d,
                  "dineroInvertido": 3000, "dineroGanado": 2500, "dineroPerdido": 1200, "vecesTodoONada": 1,
                  "empenos": 0, "prestamos": 1, "duracionSeg": 900, "comodines": %s }
                """.formatted(localNumber, result, score, jokers);
    }
}
