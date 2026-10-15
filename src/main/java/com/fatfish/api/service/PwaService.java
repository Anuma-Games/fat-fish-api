package com.fatfish.api.service;

import com.fatfish.api.dto.pwa.FeedbackRequest;
import com.fatfish.api.dto.pwa.FeedbackResponse;
import com.fatfish.api.dto.pwa.MatchDto;
import com.fatfish.api.dto.pwa.ProfileDto;
import com.fatfish.api.dto.pwa.PwaPage;
import com.fatfish.api.exception.PlayerNotFoundException;
import com.fatfish.api.model.Feedback;
import com.fatfish.api.model.PwaMatch;
import com.fatfish.api.model.PwaPlayer;
import com.fatfish.api.repository.FeedbackRepository;
import com.fatfish.api.repository.PwaMatchRepository;
import com.fatfish.api.repository.PwaPlayerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read and insert operations used by the PWA ({@code /api/pwa}), over the tables jugador, partida and retroalimentacion. */
@Service
public class PwaService {

    private final PwaPlayerRepository playerRepository;
    private final PwaMatchRepository matchRepository;
    private final FeedbackRepository feedbackRepository;

    public PwaService(PwaPlayerRepository playerRepository, PwaMatchRepository matchRepository,
            FeedbackRepository feedbackRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /** Profile of a player with the six statistics computed from all its matches. */
    @Transactional(readOnly = true)
    public ProfileDto getProfile(long playerId) {
        PwaPlayer player = findPlayer(playerId);
        List<PwaMatch> matches = matchRepository.findByPlayerIdOrderByCreatedAtAsc(playerId);
        long won = 0;
        long invested = 0;
        long lost = 0;
        long seconds = 0;
        int streak = 0;
        int maxStreak = 0;
        Instant lastActivity = null;
        for (PwaMatch match : matches) {
            invested += match.getMoneyInvested();
            lost += match.getMoneyLost();
            seconds += match.getDurationSeconds();
            // Losing streak: consecutive defeats in chronological order (an abandoned match does not break it)
            switch (match.getResult()) {
                case "VICTORIA" -> {
                    won++;
                    streak = 0;
                }
                case "DERROTA" -> maxStreak = Math.max(maxStreak, ++streak);
                default -> {
                }
            }
            lastActivity = match.getCreatedAt();
        }
        return new ProfileDto(
                String.valueOf(player.getId()),
                player.getAlias(),
                player.getCreatedAt(),
                lastActivity,
                new ProfileDto.Statistics(matches.size(), won, invested, lost, maxStreak, seconds),
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

    private PwaPlayer findPlayer(long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() -> new PlayerNotFoundException(playerId));
    }

    static MatchDto toMatch(PwaMatch match) {
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
