package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Player registered from the game (table "jugador"). Only the SHA-256 of its token is stored. */
@Entity
@Table(name = "jugador")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alias", nullable = false, length = 24)
    private String alias;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "creado_en", nullable = false)
    private Instant createdAt;

    @Column(name = "ultima_actividad")
    private Instant lastActivity;

    protected Player() {
    }

    public Player(String alias, String tokenHash) {
        this.alias = alias;
        this.tokenHash = tokenHash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Instant lastActivity) {
        this.lastActivity = lastActivity;
    }
}
