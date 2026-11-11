package com.fatfish.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bloqueo temporal del inicio de sesión por jugador: tras varios fallos dentro de la misma ventana, el id queda
 * bloqueado un tiempo sin importar la IP. La ventana y el bloqueo duran lo mismo. Cuenta también los ids que no
 * existen, para no revelar cuáles existen. Vive en memoria porque la API corre en una sola instancia. Con
 * enabled=false nunca bloquea.
 */
@Component
public class LoginAttemptService {

    /** Desde este tamaño se borran las entradas vencidas; los bloqueos vigentes nunca se borran. */
    private static final int PRUNE_THRESHOLD = 10_000;

    private static final class Attempts {
        final Instant windowEnd;
        int failures;
        Instant blockedUntil;

        Attempts(Instant windowEnd) {
            this.windowEnd = windowEnd;
        }

        boolean expired(Instant now) {
            return !now.isBefore(blockedUntil != null ? blockedUntil : windowEnd);
        }
    }

    private final boolean enabled;
    private final int maxAttempts;
    private final Duration blockDuration;
    private final Clock clock;
    private final Map<Long, Attempts> attempts = new HashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${fatfish.security.login-lockout.enabled:false}") boolean enabled,
            @Value("${fatfish.security.login-lockout.max-attempts:5}") int maxAttempts,
            @Value("${fatfish.security.login-lockout.minutes:15}") int minutes) {
        this(enabled, maxAttempts, minutes, Clock.systemUTC());
    }

    LoginAttemptService(boolean enabled, int maxAttempts, int minutes, Clock clock) {
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.blockDuration = Duration.ofMinutes(minutes);
        this.clock = clock;
    }

    public synchronized boolean isBlocked(long playerId) {
        Attempts found = attempts.get(playerId);
        return found != null && found.blockedUntil != null && clock.instant().isBefore(found.blockedUntil);
    }

    public synchronized void recordFailure(long playerId) {
        if (!enabled) {
            return;
        }
        Instant now = clock.instant();
        if (attempts.size() >= PRUNE_THRESHOLD) {
            attempts.values().removeIf(entry -> entry.expired(now));
        }
        Attempts found = attempts.get(playerId);
        if (found == null || found.expired(now)) {
            found = new Attempts(now.plus(blockDuration));
            attempts.put(playerId, found);
        }
        if (++found.failures >= maxAttempts) {
            found.blockedUntil = now.plus(blockDuration);
        }
    }

    public synchronized void reset(long playerId) {
        attempts.remove(playerId);
    }
}
