package com.fatfish.api.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One finished run of the game. Immutable once stored. */
@Entity
@Table(name = "runs")
public class Run extends AssignedIdEntity {

    @Id
    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "installation_id", nullable = false)
    private UUID installationId;

    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "game_version", nullable = false, length = 20)
    private String gameVersion;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @Column(name = "result", nullable = false, length = 20)
    private String result;

    @Column(name = "level_reached", nullable = false)
    private int levelReached;

    @Column(name = "rounds_cleared", nullable = false)
    private int roundsCleared;

    @Column(name = "bosses_defeated", nullable = false)
    private int bossesDefeated;

    @Column(name = "max_spin_score", nullable = false)
    private long maxSpinScore;

    @Column(name = "total_spins", nullable = false)
    private int totalSpins;

    @Column(name = "total_bet", nullable = false)
    private long totalBet;

    @Column(name = "total_won", nullable = false)
    private long totalWon;

    @Column(name = "total_lost", nullable = false)
    private long totalLost;

    @Column(name = "all_in_count", nullable = false)
    private int allInCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pawned_assets", nullable = false, columnDefinition = "jsonb")
    private List<String> pawnedAssets = new ArrayList<>();

    @Column(name = "loans_taken", nullable = false)
    private int loansTaken;

    @Column(name = "loan_total", nullable = false)
    private long loanTotal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "most_desperate_spin", columnDefinition = "jsonb")
    private DesperateSpin mostDesperateSpin;

    @ElementCollection
    @CollectionTable(name = "run_items", joinColumns = @JoinColumn(name = "run_id"))
    @OrderColumn(name = "position")
    @Column(name = "item_code", nullable = false, length = 50)
    private List<String> itemsBought = new ArrayList<>();

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected Run() {
    }

    public Run(UUID runId, UUID installationId) {
        this.runId = runId;
        this.installationId = installationId;
        this.receivedAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return runId;
    }

    public UUID getRunId() {
        return runId;
    }

    public UUID getInstallationId() {
        return installationId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getLevelReached() {
        return levelReached;
    }

    public void setLevelReached(int levelReached) {
        this.levelReached = levelReached;
    }

    public int getRoundsCleared() {
        return roundsCleared;
    }

    public void setRoundsCleared(int roundsCleared) {
        this.roundsCleared = roundsCleared;
    }

    public int getBossesDefeated() {
        return bossesDefeated;
    }

    public void setBossesDefeated(int bossesDefeated) {
        this.bossesDefeated = bossesDefeated;
    }

    public long getMaxSpinScore() {
        return maxSpinScore;
    }

    public void setMaxSpinScore(long maxSpinScore) {
        this.maxSpinScore = maxSpinScore;
    }

    public int getTotalSpins() {
        return totalSpins;
    }

    public void setTotalSpins(int totalSpins) {
        this.totalSpins = totalSpins;
    }

    public long getTotalBet() {
        return totalBet;
    }

    public void setTotalBet(long totalBet) {
        this.totalBet = totalBet;
    }

    public long getTotalWon() {
        return totalWon;
    }

    public void setTotalWon(long totalWon) {
        this.totalWon = totalWon;
    }

    public long getTotalLost() {
        return totalLost;
    }

    public void setTotalLost(long totalLost) {
        this.totalLost = totalLost;
    }

    public int getAllInCount() {
        return allInCount;
    }

    public void setAllInCount(int allInCount) {
        this.allInCount = allInCount;
    }

    public List<String> getPawnedAssets() {
        return pawnedAssets;
    }

    public void setPawnedAssets(List<String> pawnedAssets) {
        this.pawnedAssets = pawnedAssets;
    }

    public int getLoansTaken() {
        return loansTaken;
    }

    public void setLoansTaken(int loansTaken) {
        this.loansTaken = loansTaken;
    }

    public long getLoanTotal() {
        return loanTotal;
    }

    public void setLoanTotal(long loanTotal) {
        this.loanTotal = loanTotal;
    }

    public DesperateSpin getMostDesperateSpin() {
        return mostDesperateSpin;
    }

    public void setMostDesperateSpin(DesperateSpin mostDesperateSpin) {
        this.mostDesperateSpin = mostDesperateSpin;
    }

    public List<String> getItemsBought() {
        return itemsBought;
    }

    public void setItemsBought(List<String> itemsBought) {
        this.itemsBought = itemsBought;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
