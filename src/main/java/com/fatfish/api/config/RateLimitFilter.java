package com.fatfish.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita las peticiones por IP en los endpoints que escriben sin token del jugador (registro, sesion,
 * retroalimentacion y encuesta). Al pasarse responde 429 con Retry-After. Con enabled=false no hace nada
 * (local y CI). Es un contador en memoria: sirve porque la API corre en una sola instancia.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_KEYS = 10_000;

    private record Rule(int limit, Duration window) {
    }

    private static final class Window {
        final Instant end;
        int count;

        Window(Instant end) {
            this.end = end;
        }
    }

    private final boolean enabled;
    private final Map<String, Rule> rules;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();

    @Autowired
    public RateLimitFilter(
            @Value("${fatfish.security.rate-limit.enabled:false}") boolean enabled,
            @Value("${fatfish.security.rate-limit.register-per-hour:5}") int registerPerHour,
            @Value("${fatfish.security.rate-limit.login-per-minute:10}") int loginPerMinute,
            @Value("${fatfish.security.rate-limit.feedback-per-minute:10}") int feedbackPerMinute,
            @Value("${fatfish.security.rate-limit.survey-per-minute:10}") int surveyPerMinute) {
        this(enabled, registerPerHour, loginPerMinute, feedbackPerMinute, surveyPerMinute, Clock.systemUTC());
    }

    RateLimitFilter(boolean enabled, int registerPerHour, int loginPerMinute, int feedbackPerMinute,
            int surveyPerMinute, Clock clock) {
        this.enabled = enabled;
        this.clock = clock;
        this.rules = Map.of(
                "/api/juego/registro", new Rule(registerPerHour, Duration.ofHours(1)),
                "/api/juego/sesion", new Rule(loginPerMinute, Duration.ofMinutes(1)),
                "/api/pwa/retroalimentacion", new Rule(feedbackPerMinute, Duration.ofMinutes(1)),
                "/api/pwa/encuesta", new Rule(surveyPerMinute, Duration.ofMinutes(1)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !rules.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        long retryAfter = consume(clientIp(request) + " " + path, rules.get(path));
        if (retryAfter > 0) {
            response.setStatus(429);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"title\":\"Demasiadas peticiones\",\"status\":429,"
                    + "\"detail\":\"Superaste el límite de peticiones; intenta de nuevo en " + retryAfter
                    + " segundos\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Cuenta la petición; devuelve 0 si pasa, o los segundos que faltan para que termine la ventana. */
    private synchronized long consume(String key, Rule rule) {
        Instant now = clock.instant();
        if (windows.size() > MAX_KEYS) {
            windows.values().removeIf(window -> !now.isBefore(window.end));
            if (windows.size() > MAX_KEYS) {
                windows.clear();
            }
        }
        Window window = windows.get(key);
        if (window == null || !now.isBefore(window.end)) {
            window = new Window(now.plus(rule.window()));
            windows.put(key, window);
        }
        if (window.count < rule.limit()) {
            window.count++;
            return 0;
        }
        return Math.max(1, (Duration.between(now, window.end).toMillis() + 999) / 1000);
    }

    /** Cloud Run manda la IP del cliente en X-Forwarded-For: se toma la primera, no getRemoteAddr(). */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}