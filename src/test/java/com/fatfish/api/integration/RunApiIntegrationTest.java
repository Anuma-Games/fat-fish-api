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
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end tests against a real PostgreSQL (profile "test", connection from DB_URL / DB_USER / DB_PASSWORD).
 * Flyway migrates the schema on startup; every test starts with empty tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RunApiIntegrationTest {

    private static final String RUN_TEMPLATE = """
            {
              "schemaVersion": 1, "runId": "%s", "installationId": "%s", "playerId": "%s",
              "gameVersion": "0.3.0", "startedAt": "%s", "endedAt": "%s",
              "result": "%s", "levelReached": %d, "roundsCleared": 4, "bossesDefeated": 1,
              "maxSpinScore": %d, "totalSpins": 17, "totalBet": %d, "totalWon": %d, "totalLost": %d,
              "allInCount": 1, "pawnedAssets": ["car", "watch"], "loansTaken": 1, "loanTotal": 500,
              "mostDesperateSpin": { "level": 2, "round": 3, "spin": 4, "bet": 900, "moneyBefore": 900, "allIn": true },
              "itemsBought": ["fresh_paint", "glass_chip"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID installationId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private final UUID firstRun = UUID.randomUUID();
    private final UUID secondRun = UUID.randomUUID();

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE run_items, runs, installations, players CASCADE");
    }

    @Test
    void sendingTheSameBatchTwiceDoesNotCreateDuplicates() throws Exception {
        String batch = batch(
                run(firstRun, "2026-10-05T18:20:11Z", "2026-10-05T18:41:52Z", "defeat", 2, 144, 2350, 1800, 1450),
                run(secondRun, "2026-10-06T10:00:00Z", "2026-10-06T10:30:00Z", "demo_victory", 3, 210, 4000, 5200, 900));

        postBatch(batch)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted.length()").value(2))
                .andExpect(jsonPath("$.duplicates").isEmpty())
                .andExpect(jsonPath("$.rejected").isEmpty());

        postBatch(batch)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").isEmpty())
                .andExpect(jsonPath("$.duplicates.length()").value(2))
                .andExpect(jsonPath("$.rejected").isEmpty());

        assertThat(count("runs")).isEqualTo(2);
        assertThat(count("run_items")).isEqualTo(4);
        assertThat(count("installations")).isEqualTo(1);
        assertThat(count("players")).isEqualTo(1);
    }

    @Test
    void invalidRunsAreRejectedAndNotStored() throws Exception {
        String batch = batch(
                run(firstRun, "2026-10-05T18:20:11Z", "2026-10-05T18:41:52Z", "defeat", 2, 144, 2350, 1800, 1450),
                run(secondRun, "2026-10-05T18:20:11Z", "2026-10-05T18:00:00Z", "won", 7, 10, -1, 0, 0));

        postBatch(batch)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted[0]").value(firstRun.toString()))
                .andExpect(jsonPath("$.rejected[0].index").value(1))
                .andExpect(jsonPath("$.rejected[0].runId").value(secondRun.toString()))
                .andExpect(jsonPath("$.rejected[0].reasons.length()").value(4));

        assertThat(count("runs")).isEqualTo(1);
    }

    @Test
    void historyIsPagedFromMostRecentAndKeepsJsonFields() throws Exception {
        postBatch(batch(
                run(firstRun, "2026-10-05T18:20:11Z", "2026-10-05T18:41:52Z", "defeat", 2, 144, 2350, 1800, 1450),
                run(secondRun, "2026-10-06T10:00:00Z", "2026-10-06T10:30:00Z", "demo_victory", 3, 210, 4000, 5200, 900)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/players/{id}/runs", playerId).param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].runId").value(secondRun.toString()))
                .andExpect(jsonPath("$.content[0].pawnedAssets[1]").value("watch"))
                .andExpect(jsonPath("$.content[0].mostDesperateSpin.bet").value(900))
                .andExpect(jsonPath("$.content[0].itemsBought[0]").value("fresh_paint"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/players/{id}/runs", playerId).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].runId").value(firstRun.toString()));
    }

    @Test
    void statsAggregateAllRunsOfThePlayer() throws Exception {
        postBatch(batch(
                run(firstRun, "2026-10-05T18:20:11Z", "2026-10-05T18:41:52Z", "defeat", 2, 144, 2350, 1800, 1450),
                run(secondRun, "2026-10-06T10:00:00Z", "2026-10-06T10:30:00Z", "demo_victory", 3, 210, 4000, 5200, 900)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/players/{id}/stats", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runsPlayed").value(2))
                .andExpect(jsonPath("$.maxLevelReached").value(3))
                .andExpect(jsonPath("$.maxSpinScore").value(210))
                .andExpect(jsonPath("$.totalBet").value(6350))
                .andExpect(jsonPath("$.totalWon").value(7000))
                .andExpect(jsonPath("$.totalLost").value(2350));
    }

    @Test
    void unknownPlayerReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/players/{id}/stats", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Jugador no encontrado"));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private ResultActions postBatch(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/runs").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String run(UUID runId, String startedAt, String endedAt, String result, int level, long maxSpinScore,
            long totalBet, long totalWon, long totalLost) {
        return RUN_TEMPLATE.formatted(runId, installationId, playerId, startedAt, endedAt, result, level,
                maxSpinScore, totalBet, totalWon, totalLost);
    }

    private static String batch(String... runs) {
        return "{ \"runs\": [" + String.join(",", runs) + "] }";
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
