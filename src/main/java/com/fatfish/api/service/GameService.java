package com.fatfish.api.service;

import com.fatfish.api.dto.juego.ChangePasswordRequest;
import com.fatfish.api.dto.juego.JokerUsageRequest;
import com.fatfish.api.dto.juego.LoginRequest;
import com.fatfish.api.dto.juego.MatchRequest;
import com.fatfish.api.dto.juego.MatchResponse;
import com.fatfish.api.dto.juego.RegisterRequest;
import com.fatfish.api.dto.juego.SessionResponse;
import com.fatfish.api.exception.InvalidCredentialsException;
import com.fatfish.api.exception.InvalidMatchException;
import com.fatfish.api.exception.InvalidTokenException;
import com.fatfish.api.model.JokerUsage;
import com.fatfish.api.model.Match;
import com.fatfish.api.model.Player;
import com.fatfish.api.model.PlayerStatistics;
import com.fatfish.api.repository.JokerRepository;
import com.fatfish.api.repository.MatchRepository;
import com.fatfish.api.repository.PlayerRepository;
import com.fatfish.api.repository.PlayerStatisticsRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operations used by the game ({@code /api/juego}): registration, login, password change and match upload. */
@Service
public class GameService {

    private static final String BEARER = "Bearer ";

    private final PlayerRepository playerRepository;
    private final PlayerStatisticsRepository statisticsRepository;
    private final MatchRepository matchRepository;
    private final JokerRepository jokerRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public GameService(PlayerRepository playerRepository, PlayerStatisticsRepository statisticsRepository,
            MatchRepository matchRepository, JokerRepository jokerRepository, TokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.statisticsRepository = statisticsRepository;
        this.matchRepository = matchRepository;
        this.jokerRepository = jokerRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates the player with empty statistics and returns its id and token. */
    @Transactional
    public SessionResponse register(RegisterRequest request) {
        String token = tokenService.newToken();
        Player player = playerRepository.save(new Player(
                request.alias().strip(), passwordEncoder.encode(request.contrasena()), tokenService.hash(token)));
        statisticsRepository.save(new PlayerStatistics(player.getId()));
        return new SessionResponse(String.valueOf(player.getId()), token);
    }

    /**
     * Checks the id and password and gives the device a new token; the previous token stops working.
     * An unknown id and a wrong password get the same error so the response does not reveal which ids exist.
     */
    @Transactional
    public SessionResponse login(LoginRequest request) {
        Player player = playerRepository.findById(request.jugadorId())
                .filter(found -> passwordEncoder.matches(request.contrasena(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        String token = tokenService.newToken();
        player.setTokenHash(tokenService.hash(token));
        player.setLastActivity(Instant.now());
        return new SessionResponse(String.valueOf(player.getId()), token);
    }

    /** Changes the password of the token owner without asking for the old one (the token proves it is the owner). */
    @Transactional
    public void changePassword(String authorization, ChangePasswordRequest request) {
        Player player = authenticate(authorization);
        player.setPasswordHash(passwordEncoder.encode(request.contrasenaNueva()));
    }

    /**
     * Stores a match once per local number of the player and recalculates its statistics. A retry
     * with the same local number returns the stored match instead of inserting it again.
     */
    @Transactional
    public MatchResult recordMatch(String authorization, MatchRequest request) {
        Player player = authenticate(authorization);
        Optional<Match> existing = matchRepository.findByPlayerIdAndLocalNumber(player.getId(), request.numeroLocal());
        if (existing.isPresent()) {
            return new MatchResult(toResponse(existing.get()), false);
        }
        Match match = matchRepository.save(new Match(
                player.getId(),
                request.numeroLocal(),
                request.resultado(),
                request.rondaAlcanzada(),
                request.puntaje(),
                request.dineroInvertido(),
                request.dineroGanado(),
                request.dineroPerdido(),
                request.vecesTodoONada(),
                request.empenos(),
                request.prestamos(),
                request.duracionSeg(),
                toJokerUsages(request.comodines())));
        player.setLastActivity(match.getCreatedAt());
        PlayerStatistics statistics = statisticsRepository.findById(player.getId())
                .orElseGet(() -> new PlayerStatistics(player.getId()));
        recalculate(statistics, matchRepository.findByPlayerIdOrderByLocalNumberAsc(player.getId()));
        statisticsRepository.save(statistics);
        return new MatchResult(toResponse(match), true);
    }

    public record MatchResult(MatchResponse match, boolean created) {
    }

    /** Finds the player that owns the token of an {@code Authorization: Bearer <token>} header. */
    Player authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new InvalidTokenException();
        }
        String token = authorization.substring(BEARER.length()).strip();
        if (token.isEmpty()) {
            throw new InvalidTokenException();
        }
        return playerRepository.findByTokenHash(tokenService.hash(token)).orElseThrow(InvalidTokenException::new);
    }

    private List<JokerUsage> toJokerUsages(List<JokerUsageRequest> jokers) {
        if (jokers == null || jokers.isEmpty()) {
            return List.of();
        }
        Set<Short> ids = new HashSet<>();
        for (JokerUsageRequest joker : jokers) {
            if (!ids.add(joker.comodinId().shortValue())) {
                throw new InvalidMatchException("El comodín " + joker.comodinId() + " aparece más de una vez");
            }
        }
        if (jokerRepository.findAllById(ids).size() != ids.size()) {
            throw new InvalidMatchException("La partida incluye un comodín que no existe");
        }
        return jokers.stream()
                .map(joker -> new JokerUsage(joker.comodinId().shortValue(), joker.vecesComprado(), joker.vecesUsado()))
                .toList();
    }

    /**
     * Recalculates the totals from all the matches of the player, in the order the game numbered them,
     * so the losing streak is right even when an offline match arrives late. The best score only
     * counts victories; on a tie the one stored first is kept.
     */
    static void recalculate(PlayerStatistics statistics, List<Match> matches) {
        int bestScore = 0;
        Instant bestScoreAt = null;
        int won = 0;
        int invested = 0;
        int moneyWon = 0;
        int lost = 0;
        int seconds = 0;
        int streak = 0;
        int longestStreak = 0;
        for (Match match : matches) {
            invested += match.getMoneyInvested();
            moneyWon += match.getMoneyWon();
            lost += match.getMoneyLost();
            seconds += match.getDurationSeconds();
            // Losing streak: consecutive defeats (an abandoned match does not break it)
            switch (match.getResult()) {
                case "VICTORIA" -> {
                    won++;
                    streak = 0;
                    boolean better = bestScoreAt == null || match.getScore() > bestScore
                            || (match.getScore() == bestScore && match.getCreatedAt().isBefore(bestScoreAt));
                    if (better) {
                        bestScore = match.getScore();
                        bestScoreAt = match.getCreatedAt();
                    }
                }
                case "DERROTA" -> longestStreak = Math.max(longestStreak, ++streak);
                default -> {
                }
            }
        }
        statistics.update(bestScore, bestScoreAt, matches.size(), won, invested, moneyWon, lost, longestStreak,
                seconds);
    }

    private static MatchResponse toResponse(Match match) {
        return new MatchResponse(String.valueOf(match.getId()), match.getLocalNumber(), match.getCreatedAt());
    }
}
