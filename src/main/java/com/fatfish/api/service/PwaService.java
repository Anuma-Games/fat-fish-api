package com.fatfish.api.service;

import com.fatfish.api.dto.pwa.FeedbackRequest;
import com.fatfish.api.dto.pwa.FeedbackResponse;
import com.fatfish.api.dto.pwa.MatchDto;
import com.fatfish.api.dto.pwa.ProfileDto;
import com.fatfish.api.dto.pwa.PwaPage;
import com.fatfish.api.exception.PlayerNotFoundException;
import com.fatfish.api.model.Feedback;
import com.fatfish.api.model.Match;
import com.fatfish.api.model.Player;
import com.fatfish.api.model.PlayerStatistics;
import com.fatfish.api.repository.FeedbackRepository;
import com.fatfish.api.repository.MatchRepository;
import com.fatfish.api.repository.PlayerRepository;
import com.fatfish.api.repository.PlayerStatisticsRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read and insert operations used by the PWA ({@code /api/pwa}), over the tables jugador, estadistica_jugador, partida and retroalimentacion. */
@Service
public class PwaService {

    private final PlayerRepository playerRepository;
    private final PlayerStatisticsRepository statisticsRepository;
    private final MatchRepository matchRepository;
    private final FeedbackRepository feedbackRepository;

    public PwaService(PlayerRepository playerRepository, PlayerStatisticsRepository statisticsRepository,
            MatchRepository matchRepository, FeedbackRepository feedbackRepository) {
        this.playerRepository = playerRepository;
        this.statisticsRepository = statisticsRepository;
        this.matchRepository = matchRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /** Profile of a player with the six statistics of the record, read from estadistica_jugador. */
    @Transactional(readOnly = true)
    public ProfileDto getProfile(long playerId) {
        Player player = findPlayer(playerId);
        PlayerStatistics statistics = statisticsRepository.findById(playerId)
                .orElseGet(() -> new PlayerStatistics(playerId));
        return new ProfileDto(
                String.valueOf(player.getId()),
                player.getAlias(),
                player.getCreatedAt(),
                player.getLastActivity(),
                new ProfileDto.Statistics(
                        statistics.getTotalMatches(),
                        statistics.getMatchesWon(),
                        statistics.getMoneyInvested(),
                        statistics.getMoneyLost(),
                        statistics.getLongestLosingStreak(),
                        statistics.getSecondsPlayed()),
                Instant.now());
    }

    /** Match history of a player, most recent first. */
    @Transactional(readOnly = true)
    public PwaPage<MatchDto> getMatches(long playerId, int page, int size) {
        findPlayer(playerId);
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PwaPage.from(matchRepository.findByPlayerId(playerId, pageRequest), PwaService::toMatch);
    }

    /**
     * Stores a feedback once per idempotency key. A retry with the same key returns the stored
     * feedback instead of inserting it again.
     */
    @Transactional
    public FeedbackResult createFeedback(UUID idempotencyKey, FeedbackRequest request) {
        Optional<Feedback> existing = feedbackRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new FeedbackResult(toResponse(existing.get()), false);
        }
        Feedback saved = feedbackRepository.save(new Feedback(
                idempotencyKey, request.categoria(), request.componente(), request.mensaje().strip()));
        return new FeedbackResult(toResponse(saved), true);
    }

    public record FeedbackResult(FeedbackResponse feedback, boolean created) {
    }

    private Player findPlayer(long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() -> new PlayerNotFoundException(playerId));
    }

    static MatchDto toMatch(Match match) {
        return new MatchDto(
                String.valueOf(match.getId()),
                match.getResult(),
                match.getRoundReached(),
                match.getScore(),
                match.getMoneyInvested(),
                match.getMoneyWon(),
                match.getMoneyLost(),
                match.getDurationSeconds(),
                match.getCreatedAt());
    }

    private static FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(String.valueOf(feedback.getId()), feedback.getCreatedAt());
    }
}
