package com.fatfish.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** How many times a joker was bought and used in one match (row of "partida_comodin"). */
@Embeddable
public class JokerUsage {

    @Column(name = "comodin_id", nullable = false)
    private Short jokerId;

    @Column(name = "veces_comprado", nullable = false)
    private int timesBought;

    @Column(name = "veces_usado", nullable = false)
    private int timesUsed;

    protected JokerUsage() {
    }

    public JokerUsage(Short jokerId, int timesBought, int timesUsed) {
        this.jokerId = jokerId;
        this.timesBought = timesBought;
        this.timesUsed = timesUsed;
    }

    public Short getJokerId() {
        return jokerId;
    }

    public int getTimesBought() {
        return timesBought;
    }

    public int getTimesUsed() {
        return timesUsed;
    }
}
