package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Feedback sent from the PWA. Insert only and anonymous. */
@Entity
@Table(name = "retroalimentacion")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_idempotencia", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "categoria", nullable = false, length = 20)
    private String category;

    @Column(name = "componente", nullable = false, length = 20)
    private String component;

    @Column(name = "mensaje", nullable = false, length = 1000)
    private String message;

    @Column(name = "creada_en", nullable = false)
    private Instant createdAt;

    protected Feedback() {
    }

    public Feedback(UUID idempotencyKey, String category, String component, String message) {
        this.idempotencyKey = idempotencyKey;
        this.category = category;
        this.component = component;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCategory() {
        return category;
    }

    public String getComponent() {
        return component;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
