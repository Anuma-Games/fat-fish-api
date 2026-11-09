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
 * Bloqueo temporal del inicio de sesión por jugador: tras varios fallos seguidos, el id queda bloqueado un
 * tiempo sin importar la IP. Cuenta también los ids que no existen, para no revelar cuáles existen. Vive en
 * memoria porque la API corre en una sola instancia. Con enabled=false nunca bloquea.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_TRACKED = 10_000;

    private static final class Attempts {
        int failures;
        Instant blockedUntil;
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
        if (found == null || found.blockedUntil == null) {
            return false;
        }
        if (clock.instant().isBefore(found.blockedUntil)) {
            return true;
        }
        attempts.remove(playerId);
        return false;
    }

    public synchronized void recordFailure(long playerId) {
        if (!enabled) {
            return;
        }
        if (attempts.size() >= MAX_TRACKED) {
            attempts.clear();
        }
        Attempts found = attempts.computeIfAbsent(playerId, id -> new Attempts());
        if (++found.failures >= maxAttempts) {
            found.failures = 0;
            found.blockedUntil = clock.instant().plus(blockDuration);
        }
    }

    public synchronized void reset(long playerId) {
        attempts.remove(playerId);
    }
}