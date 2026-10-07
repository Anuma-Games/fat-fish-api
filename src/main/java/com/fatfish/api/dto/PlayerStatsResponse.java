package com.fatfish.api.dto;

import java.util.UUID;

public record PlayerStatsResponse(
        UUID playerId,
        long runsPlayed,
        int maxLevelReached,
        long maxSpinScore,
        long totalBet,
        long totalWon,
        long totalLost) {
}
