package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player extends AssignedIdEntity {

    @Id
    private UUID id;

    @Column(name = "display_name", length = 60)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Player() {
    }

    public Player(UUID id) {
        this.id = id;
        this.createdAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
