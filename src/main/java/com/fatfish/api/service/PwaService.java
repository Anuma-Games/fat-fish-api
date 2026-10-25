package com.fatfish.api.service;

import com.fatfish.api.dto.pwa.FeedbackRequest;
import com.fatfish.api.dto.pwa.FeedbackResponse;
import com.fatfish.api.dto.pwa.MatchDto;
import com.fatfish.api.dto.pwa.PlayerSummaryDto;
import com.fatfish.api.dto.pwa.ProfileDto;
import com.fatfish.api.dto.pwa.PwaPage;
import com.fatfish.api.dto.pwa.RankingDto;
import com.fatfish.api.dto.pwa.RankingEntryDto;
import com.fatfish.api.dto.pwa.SurveyRequest;
import com.fatfish.api.dto.pwa.SurveyResponse;
import com.fatfish.api.exception.PlayerNotFoundException;
import com.fatfish.api.model.Feedback;
import com.fatfish.api.model.Match;
import com.fatfish.api.model.Player;
import com.fatfish.api.model.PlayerStatistics;
import com.fatfish.api.model.Survey;
import com.fatfish.api.repository.FeedbackRepository;
import com.fatfish.api.repository.MatchRepository;
import com.fatfish.api.repository.PlayerRepository;
import com.fatfish.api.repository.PlayerStatisticsRepository;
import com.fatfish.api.repository.PlayerStatisticsRepository.RankingRow;
import com.fatfish.api.repository.SurveyRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read and insert operations used by the PWA ({@code /api/pwa}), over the tables jugador, estadistica_jugador,
 * partida, retroalimentacion and encuesta.
 */
@Service
public class PwaService {

    private static final Pattern DIGITS = Pattern.compile("\\d{1,18}");
    private static final int MAX_SEARCH_RESULTS = 20;
    private static final int RANKING_SIZE = 50;

    private final PlayerRepository playerRepository;
    private final PlayerStatisticsRepository statisticsRepository;
    private final MatchRepository matchRepository;
    private final FeedbackRepository feedbackRepository;
    private final SurveyRepository surveyRepository;

    public PwaService(PlayerRepository playerRepository, PlayerStatisticsRepository statisticsRepository,
            MatchRepository matchRepository, FeedbackRepository feedbackRepository,
            SurveyRepository surveyRepository) {
        this.playerRepository = playerRepository;
        this.statisticsRepository = statisticsRepository;
        this.matchRepository = matchRepository;
        this.feedbackRepository = feedbackRepository;
        this.surveyRepository = surveyRepository;
    }

    /**
     * Players whose id is exactly the search text (only digits) or whose alias contains it, ignoring case. The id
     * match goes first; then by last activity. Runs in one transaction so both queries return the same
     * {@link Player} instance and {@code distinct()} removes the repeated one.
     */
    @Transactional(readOnly = true)
    public List<PlayerSummaryDto> searchPlayers(String search) {
        String term = search.strip();
        Stream<Player> byId = DIGITS.matcher(term).matches()
                ? playerRepository.findById(Long.parseLong(term)).stream()
                : Stream.empty();
        Stream<Player> byAlias = playerRepository.searchByAlias(term, Limit.of(MAX_SEARCH_RESULTS)).stream();
        return Stream.concat(byId, byAlias)
                .distinct()
                .limit(MAX_SEARCH_RESULTS)
                .map(PwaService::toSummary)
                .toList();
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
                Sort.by(Sort.Order.desc("finishedAt"), Sort.Order.desc("id")));
        return PwaPage.from(matchRepository.findByPlayerId(playerId, pageRequest), PwaService::toMatch);
    }

    /**
     * World ranking: best winning score of each player, up to position 50. Ties go to whoever reached the score
     * first, the same rule the game uses to store it. {@code miPosicion} is the real position of the player, even
     * outside the top 50.
     */
    @Transactional(readOnly = true)
    public RankingDto getRanking(int page, int size, Long playerId) {
        Page<RankingRow> rows = statisticsRepository.findRanking(PageRequest.of(page, size));
        long firstPosition = rows.getPageable().getOffset() + 1;
        // The query does not know the top 50: a page that crosses position 50 is cut, one after it stays empty
        long slotsLeft = Math.max(0, RANKING_SIZE - rows.getPageable().getOffset());
        List<RankingRow> inTop = rows.getContent().stream().limit(slotsLeft).toList();
        List<RankingEntryDto> entries = IntStream.range(0, inTop.size())
                .mapToObj(i -> toRankingEntry(firstPosition + i, inTop.get(i)))
                .toList();
        long total = Math.min(rows.getTotalElements(), RANKING_SIZE);
        int totalPages = (int) ((total + size - 1) / size);
        PwaPage<RankingEntryDto> ranking = new PwaPage<>(
                entries, page, size, total, totalPages, page + 1 >= totalPages, Instant.now());
        return new RankingDto(ranking, playerId == null ? null : findPosition(playerId));
    }

    /**
     * Stores a feedback once per idempotency key. A retry with the same key returns the stored
     * feedback instead of inserting it again.
     *
     * <p>Not {@code @Transactional} on purpose: each repository call runs in its own transaction. When two
     * retries with the same key overlap, the losing insert fails on the unique key, and the second read finds
     * the row the other request already committed.
     */
    public FeedbackResult createFeedback(UUID idempotencyKey, FeedbackRequest request) {
        Optional<Feedback> existing = feedbackRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new FeedbackResult(toResponse(existing.get()), false);
        }
        try {
            Feedback saved = feedbackRepository.save(new Feedback(
                    idempotencyKey, request.categoria(), request.componente(), request.mensaje().strip()));
            return new FeedbackResult(toResponse(saved), true);
        } catch (DataIntegrityViolationException e) {
            Feedback stored = feedbackRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
            return new FeedbackResult(toResponse(stored), false);
        }
    }

    /**
     * Stores an anonymous survey once per idempotency key, like {@link #createFeedback} (also without
     * {@code @Transactional}, for the same reason). A blank comment is stored as null.
     */
    public SurveyResult createSurvey(UUID idempotencyKey, SurveyRequest request) {
        Optional<Survey> existing = surveyRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new SurveyResult(toResponse(existing.get(), false), false);
        }
        try {
            Survey saved = surveyRepository.save(new Survey(
                    idempotencyKey,
                    request.percepcionRiesgo().shortValue(),
                    request.cambioOpinion().shortValue(),
                    request.claridadMensaje().shortValue(),
                    blankToNull(request.comentario())));
            return new SurveyResult(toResponse(saved, true), true);
        } catch (DataIntegrityViolationException e) {
            Survey stored = surveyRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
            return new SurveyResult(toResponse(stored, false), false);
        }
    }

    public record FeedbackResult(FeedbackResponse feedback, boolean created) {
    }

    public record SurveyResult(SurveyResponse survey, boolean created) {
    }

    private Player findPlayer(long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() -> new PlayerNotFoundException(playerId));
    }

    private RankingEntryDto findPosition(long playerId) {
        return statisticsRepository.findRankingRow(playerId)
                .map(row -> toRankingEntry(
                        statisticsRepository.countAhead(row.getBestScore(), row.getBestScoreAt(), row.getPlayerId()) + 1,
                        row))
                .orElse(null);
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
                match.getFinishedAt());
    }

    private static PlayerSummaryDto toSummary(Player player) {
        return new PlayerSummaryDto(String.valueOf(player.getId()), player.getAlias(), player.getLastActivity());
    }

    private static RankingEntryDto toRankingEntry(long position, RankingRow row) {
        return new RankingEntryDto(position, String.valueOf(row.getPlayerId()), row.getAlias(), row.getBestScore(),
                row.getBestScoreAt());
    }

    private static FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(String.valueOf(feedback.getId()), feedback.getCreatedAt());
    }

    private static SurveyResponse toResponse(Survey survey, boolean created) {
        return new SurveyResponse(String.valueOf(survey.getId()), created, survey.getSentAt());
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
