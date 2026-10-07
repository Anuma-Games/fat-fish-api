package com.fatfish.api.service;

import com.fatfish.api.dto.PageResponse;
import com.fatfish.api.dto.PlayerStatsResponse;
import com.fatfish.api.dto.RunRecord;
import com.fatfish.api.exception.PlayerNotFoundException;
import com.fatfish.api.repository.PlayerRepository;
import com.fatfish.api.repository.PlayerStatsView;
import com.fatfish.api.repository.RunRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final RunRepository runRepository;

    public PlayerService(PlayerRepository playerRepository, RunRepository runRepository) {
        this.playerRepository = playerRepository;
        this.runRepository = runRepository;
    }

    /** Run history, most recent first. */
    public PageResponse<RunRecord> getRuns(UUID playerId, int page, int size) {
        ensurePlayerExists(playerId);
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("runId")));
        return PageResponse.from(runRepository.findByPlayerId(playerId, pageRequest), RunMapper::toRecord);
    }

    public PlayerStatsResponse getStats(UUID playerId) {
        ensurePlayerExists(playerId);
        PlayerStatsView stats = runRepository.aggregateByPlayer(playerId);
        if (stats == null) {
            return new PlayerStatsResponse(playerId, 0, 0, 0, 0, 0, 0);
        }
        return new PlayerStatsResponse(
                playerId,
                zeroIfNull(stats.getRunsPlayed()),
                stats.getMaxLevelReached() == null ? 0 : stats.getMaxLevelReached(),
                zeroIfNull(stats.getMaxSpinScore()),
                zeroIfNull(stats.getTotalBet()),
                zeroIfNull(stats.getTotalWon()),
                zeroIfNull(stats.getTotalLost()));
    }

    private void ensurePlayerExists(UUID playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
    }

    private static long zeroIfNull(Long value) {
        return value == null ? 0 : value;
    }
}
