package com.fatfish.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bloqueo temporal del inicio de sesión por jugador: tras varios intentos fallidos dentro de la misma ventana, el id
 * queda bloqueado un tiempo sin importar la IP. La ventana y el bloqueo duran lo mismo. Cuenta también los ids que no
 * existen, para no revelar cuáles existen. Vive en memoria porque la API corre en una sola instancia. Con
 * enabled=false nunca bloquea.
 *
 * <p>Cada intento se cuenta antes de revisar la contraseña ({@link #tryAcquire}), así varias peticiones al mismo
 * tiempo no pueden probar más contraseñas de las permitidas. Un inicio de sesión correcto borra la cuenta
 * ({@link #reset}).
 */
@Component
public class LoginAttemptService {

    /** Máximo de ids con intentos recientes. Con el mapa lleno, los ids nuevos no se cuentan (el límite por IP sigue). */
    private static final int MAX_TRACKED = 10_000;

    private static final class Attempts {
        int count;
        boolean blocked;
        Instant expiresAt;

        Attempts(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    private final boolean enabled;
    private final int maxAttempts;
    private final Duration blockDuration;
    private final Clock clock;

    /**
     * En orden de vencimiento: toda entrada vence blockDuration después de crearse o de bloquearse, y al bloquearse
     * pasa al final. Así las vencidas siempre están al inicio y se borran sin recorrer todo el mapa.
     */
    private final Map<Long, Attempts> attempts = new LinkedHashMap<>();

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

    /**
     * Cuenta un intento de inicio de sesión del jugador. Devuelve false si está bloqueado; el intento que alcanza el
     * máximo todavía pasa y deja bloqueados a los siguientes.
     */
    public synchronized boolean tryAcquire(long playerId) {
        if (!enabled) {
            return true;
        }
        Instant now = clock.instant();
        removeExpired(now);
        Attempts found = attempts.get(playerId);
        if (found == null) {
            if (attempts.size() >= MAX_TRACKED) {
                return true;
            }
            found = new Attempts(now.plus(blockDuration));
            attempts.put(playerId, found);
        }
        if (found.blocked) {
            return false;
        }
        if (++found.count >= maxAttempts) {
            found.blocked = true;
            found.expiresAt = now.plus(blockDuration);
            attempts.remove(playerId);
            attempts.put(playerId, found);
        }
        return true;
    }

    /** True mientras dura el bloqueo del jugador. */
    public synchronized boolean isBlocked(long playerId) {
        Attempts found = attempts.get(playerId);
        return found != null && found.blocked && clock.instant().isBefore(found.expiresAt);
    }

    /** Borra los intentos del jugador después de un inicio de sesión correcto. */
    public synchronized void reset(long playerId) {
        attempts.remove(playerId);
    }

    private void removeExpired(Instant now) {
        Iterator<Attempts> iterator = attempts.values().iterator();
        while (iterator.hasNext()) {
            if (now.isBefore(iterator.next().expiresAt)) {
                return;
            }
            iterator.remove();
        }
    }
}
