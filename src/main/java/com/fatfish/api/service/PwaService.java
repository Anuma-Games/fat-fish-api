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
import com.fatfish.api.dto.pwa.PlayerSummaryDto;
import com.fatfish.api.dto.pwa.RankingDto;
import com.fatfish.api.dto.pwa.RankingEntryDto;
import com.fatfish.api.dto.pwa.SurveyRequest;
import com.fatfish.api.dto.pwa.SurveyResponse;
import com.fatfish.api.model.Survey;
import com.fatfish.api.repository.PlayerStatisticsRepository.RankingRow;
import com.fatfish.api.repository.SurveyRepository;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;

/** Read and insert operations used by the PWA ({@code /api/pwa}), over the tables jugador, estadistica_jugador, partida and retroalimentacion. */
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
    @Transactional
    public SurveyResult createSurvey(UUID idempotencyKey, SurveyRequest request) {
        Optional<Survey> existing = surveyRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new SurveyResult(toResponse(existing.get(),false), false);
        }
        Survey saved = surveyRepository.save(new Survey(
                idempotencyKey,
                request.percepcionRiesgo().shortValue(),
                request.cambioOpinion().shortValue(),
                request.claridadMensaje().shortValue(),
                blankToNull(request.comentario())));
        return new SurveyResult(toResponse(saved,true ), true);
    }
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
    @Transactional(readOnly = true)
    public RankingDto getRanking(int page, int size, Long playerId) {
        Page<RankingRow> rows = statisticsRepository.findRanking(PageRequest.of(page, size));
        long firstPosition = rows.getPageable().getOffset() + 1;
        List<RankingEntryDto> entries = IntStream.range(0, rows.getNumberOfElements())
                .mapToObj(i -> toRankingEntry(firstPosition + i, rows.getContent().get(i)))
                .toList();
        long total = Math.min(rows.getTotalElements(), RANKING_SIZE);
        int totalPages = (int) ((total + size - 1) / size);
        PwaPage<RankingEntryDto> ranking = new PwaPage<>(
                entries, page, size, total, totalPages, page + 1 >= totalPages, Instant.now());
        return new RankingDto(ranking, playerId == null ? null : findPosition(playerId));
    }
    
     public record SurveyResult(SurveyResponse survey, boolean created) {
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
                match.getFinishedAt());
    }

    private static FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(String.valueOf(feedback.getId()), feedback.getCreatedAt());
    }
     private RankingEntryDto findPosition(long playerId) {
        return statisticsRepository.findRankingRow(playerId)
                .map(row -> toRankingEntry(
                        statisticsRepository.countAhead(row.getBestScore(), row.getBestScoreAt(), row.getPlayerId()) + 1,
                        row))
                .orElse(null);
    }

    private static RankingEntryDto toRankingEntry(long position, RankingRow row) {
        return new RankingEntryDto(position, String.valueOf(row.getPlayerId()), row.getAlias(), row.getBestScore(),
                row.getBestScoreAt());
    }

    private static PlayerSummaryDto toSummary(Player player) {
        return new PlayerSummaryDto(String.valueOf(player.getId()), player.getAlias(), player.getLastActivity());
    }

    private static SurveyResponse toResponse(Survey survey, boolean created) {
        return new SurveyResponse(String.valueOf(survey.getId()), created, survey.getSentAt());
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
