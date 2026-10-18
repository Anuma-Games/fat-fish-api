package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Joker catalog (table "comodin"), loaded by the migration. Read only. */
@Entity
@Table(name = "comodin")
public class Joker {

    @Id
    private Short id;

    @Column(name = "nombre", nullable = false, unique = true, length = 40)
    private String name;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String description;

    protected Joker() {
    }

    public Short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
