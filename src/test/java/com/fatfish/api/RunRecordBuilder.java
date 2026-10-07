package com.fatfish.api;

import com.fatfish.api.dto.DesperateSpinDto;
import com.fatfish.api.dto.RunRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Test helper: builds the example RunRecord from the contract, with overridable fields. */
public final class RunRecordBuilder {

    private Integer schemaVersion = 1;
    private UUID runId = UUID.randomUUID();
    private UUID installationId = UUID.randomUUID();
    private UUID playerId;
    private String gameVersion = "0.3.0";
    private Instant startedAt = Instant.parse("2026-10-05T18:20:11Z");
    private Instant endedAt = Instant.parse("2026-10-05T18:41:52Z");
    private String result = "defeat";
    private Integer levelReached = 2;
    private Integer roundsCleared = 4;
    private Integer bossesDefeated = 1;
    private Long maxSpinScore = 144L;
    private Integer totalSpins = 17;
    private Long totalBet = 2350L;
    private Long totalWon = 1800L;
    private Long totalLost = 1450L;
    private Integer allInCount = 1;
    private List<String> pawnedAssets = List.of("car");
    private Integer loansTaken = 1;
    private Long loanTotal = 500L;
    private DesperateSpinDto mostDesperateSpin = new DesperateSpinDto(2, 3, 4, 900L, 900L, true);
    private List<String> itemsBought = List.of("fresh_paint", "glass_chip");

    public static RunRecordBuilder aRun() {
        return new RunRecordBuilder();
    }

    public RunRecordBuilder schemaVersion(Integer value) {
        this.schemaVersion = value;
        return this;
    }

    public RunRecordBuilder runId(UUID value) {
        this.runId = value;
        return this;
    }

    public RunRecordBuilder installationId(UUID value) {
        this.installationId = value;
        return this;
    }

    public RunRecordBuilder playerId(UUID value) {
        this.playerId = value;
        return this;
    }

    public RunRecordBuilder startedAt(Instant value) {
        this.startedAt = value;
        return this;
    }

    public RunRecordBuilder endedAt(Instant value) {
        this.endedAt = value;
        return this;
    }

    public RunRecordBuilder result(String value) {
        this.result = value;
        return this;
    }

    public RunRecordBuilder levelReached(Integer value) {
        this.levelReached = value;
        return this;
    }

    public RunRecordBuilder maxSpinScore(Long value) {
        this.maxSpinScore = value;
        return this;
    }

    public RunRecordBuilder totalSpins(Integer value) {
        this.totalSpins = value;
        return this;
    }

    public RunRecordBuilder totalBet(Long value) {
        this.totalBet = value;
        return this;
    }

    public RunRecordBuilder totalWon(Long value) {
        this.totalWon = value;
        return this;
    }

    public RunRecordBuilder totalLost(Long value) {
        this.totalLost = value;
        return this;
    }

    public RunRecordBuilder loanTotal(Long value) {
        this.loanTotal = value;
        return this;
    }

    public RunRecord build() {
        return new RunRecord(schemaVersion, runId, installationId, playerId, gameVersion, startedAt, endedAt,
                result, levelReached, roundsCleared, bossesDefeated, maxSpinScore, totalSpins, totalBet, totalWon,
                totalLost, allInCount, pawnedAssets, loansTaken, loanTotal, mostDesperateSpin, itemsBought);
    }
}
