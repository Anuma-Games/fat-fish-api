package com.fatfish.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Límite de peticiones por IP y bloqueo de /sesion, con los límites activados y muy bajos. */
@SpringBootTest(properties = {
        "fatfish.security.rate-limit.enabled=true",
        "fatfish.security.rate-limit.register-per-hour=2",
        "fatfish.security.rate-limit.login-per-minute=5",
        "fatfish.security.rate-limit.feedback-per-minute=2",
        "fatfish.security.rate-limit.survey-per-minute=2",
        "fatfish.security.login-lockout.enabled=true",
        "fatfish.security.login-lockout.max-attempts=3"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityLimitsIntegrationTest {

    private static final String PASSWORD = "pez-gordo-2026";
    private static final String SURVEY = "{ \"percepcionRiesgo\": 4, \"cambioOpinion\": 3, \"claridadMensaje\": 5 }";
    private static final String FEEDBACK =
            "{ \"categoria\": \"OTRO\", \"componente\": \"PWA\", \"mensaje\": \"Mensaje de prueba largo\" }";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE partida_comodin, partida, estadistica_jugador, jugador RESTART IDENTITY");
    }

    @Test
    void limitsRegistrationsPerIp() throws Exception {
        register("10.0.0.1");
        register("10.0.0.1");

        expectTooManyRequests(send("/api/juego/registro", "10.0.0.1", registerBody()));
    }

    @Test
    void limitsFeedbackAndSurveysPerIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            send("/api/pwa/encuesta", "10.0.0.2", SURVEY).andExpect(status().isCreated());
            send("/api/pwa/retroalimentacion", "10.0.0.2", FEEDBACK).andExpect(status().isCreated());
        }

        expectTooManyRequests(send("/api/pwa/encuesta", "10.0.0.2", SURVEY));
        expectTooManyRequests(send("/api/pwa/retroalimentacion", "10.0.0.2", FEEDBACK));
    }

    @Test
    void limitsLoginsPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            send("/api/juego/sesion", "10.0.0.3", login(99, "equivocada")).andExpect(status().isUnauthorized());
        }

        expectTooManyRequests(send("/api/juego/sesion", "10.0.0.3", login(99, "equivocada")));
    }

    @Test
    void locksThePlayerAfterFailedAttemptsEvenWithTheRightPassword() throws Exception {
        register("10.0.0.4");

        for (int i = 0; i < 3; i++) {
            send("/api/juego/sesion", "10.0.0.5", login(1, "equivocada")).andExpect(status().isUnauthorized());
        }

        // Bloqueado: responde igual que una contraseña incorrecta, aunque ahora sea la correcta
        send("/api/juego/sesion", "10.0.0.5", login(1, PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("El id del jugador o la contraseña no son correctos"));
    }

    @Test
    void doesNotLimitMatchesSentWithAToken() throws Exception {
        String token = register("10.0.0.6");

        for (int n = 1; n <= 5; n++) {
            mockMvc.perform(post("/api/juego/partidas").header("X-Forwarded-For", "10.0.0.6")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(match(n)))
                    .andExpect(status().isCreated());
        }
    }

    private String register(String ip) throws Exception {
        String body = send("/api/juego/registro", ip, registerBody())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions send(String path, String ip, String body) throws Exception {
        return mockMvc.perform(post(path)
                .header("X-Forwarded-For", ip).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static void expectTooManyRequests(ResultActions result) throws Exception {
        result.andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429));
    }

    private static String registerBody() {
        return "{ \"alias\": \"andrik\", \"contrasena\": \"" + PASSWORD + "\" }";
    }

    private static String login(long playerId, String password) {
        return "{ \"jugadorId\": " + playerId + ", \"contrasena\": \"" + password + "\" }";
    }

    private static String match(int n) {
        return """
                { "claveIdempotencia": "00000000-0000-4000-8000-%012d", "terminadaEn": "2026-10-0%dT10:00:00Z",
                  "resultado": "DERROTA", "rondaAlcanzada": 1, "puntaje": 0, "dineroInvertido": 0,
                  "dineroGanado": 0, "dineroPerdido": 0, "vecesTodoONada": 0, "empenos": 0, "prestamos": 0,
                  "duracionSeg": 1 }
                """.formatted(n, n);
    }
}