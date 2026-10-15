package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Player as the PWA shows it (table "jugador"). Read only from the API. */
@Entity
@Table(name = "jugador")
public class PwaPlayer {

    @Id
    private Long id;

    @Column(name = "alias", nullable = false, length = 24)
    private String alias;

    @Column(name = "creado_en", nullable = false)
    private Instant createdAt;

    protected PwaPlayer() {
    }

    public Long getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
