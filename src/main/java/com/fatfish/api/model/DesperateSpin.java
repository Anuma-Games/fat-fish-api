package com.fatfish.api.model;

/** Snapshot of the riskiest spin of a run. Stored as JSONB inside runs.most_desperate_spin. */
public record DesperateSpin(
        Integer level,
        Integer round,
        Integer spin,
        Long bet,
        Long moneyBefore,
        Boolean allIn) {
}
