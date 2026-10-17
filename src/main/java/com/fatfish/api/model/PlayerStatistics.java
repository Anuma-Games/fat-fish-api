package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Totals of a player (table "estadistica_jugador"), recalculated each time the game records a match.
 * The world ranking orders by {@code puntaje_maximo} and breaks ties with {@code puntaje_maximo_en}.
 */
@Entity
@Table(name = "estadistica_jugador")
public class PlayerStatistics {

    @Id
    @Column(name = "jugador_id")
    private Long playerId;

    @Column(name = "puntaje_maximo", nullable = false)
    private int bestScore;

    @Column(name = "puntaje_maximo_en")
    private Instant bestScoreAt;

    @Column(name = "partidas_totales", nullable = false)
    private int totalMatches;

    @Column(name = "partidas_ganadas", nullable = false)
    private int matchesWon;

    @Column(name = "dinero_invertido", nullable = false)
    private int moneyInvested;

    @Column(name = "dinero_ganado", nullable = false)
    private int moneyWon;

    @Column(name = "dinero_perdido", nullable = false)
    private int moneyLost;

    @Column(name = "racha_derrotas_max", nullable = false)
    private int longestLosingStreak;

    @Column(name = "tiempo_jugado_seg", nullable = false)
    private int secondsPlayed;

    protected PlayerStatistics() {
    }

    /** Empty statistics of a player that has not finished any match yet. */
    public PlayerStatistics(Long playerId) {
        this.playerId = playerId;
    }

    public void update(int bestScore, Instant bestScoreAt, int totalMatches, int matchesWon, int moneyInvested,
            int moneyWon, int moneyLost, int longestLosingStreak, int secondsPlayed) {
        this.bestScore = bestScore;
        this.bestScoreAt = bestScoreAt;
        this.totalMatches = totalMatches;
        this.matchesWon = matchesWon;
        this.moneyInvested = moneyInvested;
        this.moneyWon = moneyWon;
        this.moneyLost = moneyLost;
        this.longestLosingStreak = longestLosingStreak;
        this.secondsPlayed = secondsPlayed;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public int getBestScore() {
        return bestScore;
    }

    public Instant getBestScoreAt() {
        return bestScoreAt;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public int getMatchesWon() {
        return matchesWon;
    }

    public int getMoneyInvested() {
        return moneyInvested;
    }

    public int getMoneyWon() {
        return moneyWon;
    }

    public int getMoneyLost() {
        return moneyLost;
    }

    public int getLongestLosingStreak() {
        return longestLosingStreak;
    }

    public int getSecondsPlayed() {
        return secondsPlayed;
    }
}
