package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Finished run as the PWA shows it (table "partida"). Read only from the API. */
@Entity
@Table(name = "partida")
public class PwaMatch {

    @Id
    private Long id;

    @Column(name = "jugador_id", nullable = false)
    private Long playerId;

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

    @Column(name = "duracion_seg", nullable = false)
    private int durationSeconds;

    @Column(name = "creada_en", nullable = false)
    private Instant createdAt;

    protected PwaMatch() {
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
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

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
