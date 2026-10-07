package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A copy of the game on a device. Anonymous (playerId == null) until it is linked to a player. */
@Entity
@Table(name = "installations")
public class Installation extends AssignedIdEntity {

    @Id
    private UUID id;

    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Installation() {
    }

    public Installation(UUID id) {
        this.id = id;
        this.createdAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
