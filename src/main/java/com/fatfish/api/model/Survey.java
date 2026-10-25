package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "encuesta")
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_idempotencia", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "percepcion_riesgo", nullable = false)
    private short riskPerception;

    @Column(name = "cambio_opinion", nullable = false)
    private short opinionChange;

    @Column(name = "claridad_mensaje", nullable = false)
    private short messageClarity;

    @Column(name = "comentario", length = 500)
    private String comment;

    @Column(name = "enviada_en", nullable = false)
    private Instant sentAt;

    protected Survey() {
    }

    public Survey(UUID idempotencyKey, short riskPerception, short opinionChange, short messageClarity,
            String comment) {
        this.idempotencyKey = idempotencyKey;
        this.riskPerception = riskPerception;
        this.opinionChange = opinionChange;
        this.messageClarity = messageClarity;
        this.comment = comment;
        this.sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public short getRiskPerception() {
        return riskPerception;
    }

    public short getOpinionChange() {
        return opinionChange;
    }

    public short getMessageClarity() {
        return messageClarity;
    }

    public String getComment() {
        return comment;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
