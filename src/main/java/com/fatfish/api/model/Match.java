package com.fatfish.api.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Finished match sent by the game (table "partida"). {@code clave_idempotencia} is the UUID the game
 * generates when the match ends, so a retried upload does not create a second row, even when the same
 * account plays on several devices.
 */
@Entity
@Table(name = "partida")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jugador_id", nullable = false)
    private Long playerId;

    @Column(name = "clave_idempotencia", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "resultado", nullable = false, length = 10)
    private String result;

    @Column(name = "ronda_alcanzada", nullable = false)
    private int roundReached;

    @Column(name = "puntaje", nullable = false)
    private int score;

    @Column(name = "dinero_invertido", nullable = false)
    private int moneyInvested;

    @Column(name = "dinero_ganado", nullable = false)
    private int moneyWon;

    @Column(name = "dinero_perdido", nullable = false)
    private int moneyLost;

    @Column(name = "veces_todo_o_nada", nullable = false)
    private int allInCount;

    @Column(name = "empenos", nullable = false)
    private int pawns;

    @Column(name = "prestamos", nullable = false)
    private int loans;

    @Column(name = "duracion_seg", nullable = false)
    private int durationSeconds;

    @Column(name = "terminada_en", nullable = false)
    private Instant finishedAt;

    @Column(name = "creada_en", nullable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "partida_comodin", joinColumns = @JoinColumn(name = "partida_id"))
    private List<JokerUsage> jokers = new ArrayList<>();

    protected Match() {
    }

    public Match(Long playerId, UUID idempotencyKey, Instant finishedAt, String result, int roundReached, int score, int moneyInvested,
            int moneyWon, int moneyLost, int allInCount, int pawns, int loans, int durationSeconds,
            List<JokerUsage> jokers) {
        this.playerId = playerId;
        this.idempotencyKey = idempotencyKey;
        this.finishedAt = finishedAt;
        this.result = result;
        this.roundReached = roundReached;
        this.score = score;
        this.moneyInvested = moneyInvested;
        this.moneyWon = moneyWon;
        this.moneyLost = moneyLost;
        this.allInCount = allInCount;
        this.pawns = pawns;
        this.loans = loans;
        this.durationSeconds = durationSeconds;
        this.jokers = new ArrayList<>(jokers);
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getResult() {
        return result;
    }

    public int getRoundReached() {
        return roundReached;
    }

    public int getScore() {
        return score;
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

    public int getAllInCount() {
        return allInCount;
    }

    public int getPawns() {
        return pawns;
    }

    public int getLoans() {
        return loans;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<JokerUsage> getJokers() {
        return jokers;
    }
}
