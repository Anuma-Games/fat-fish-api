package com.fatfish.api.repository;

/** Raw aggregates for one player. Every value except runsPlayed is null when the player has no runs. */
public interface PlayerStatsView {

    Long getRunsPlayed();

    Integer getMaxLevelReached();

    Long getMaxSpinScore();

    Long getTotalBet();

    Long getTotalWon();

    Long getTotalLost();
}
