package com.fatfish.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Máximo de IPs con ventana activa por endpoint. Con el mapa lleno, las IPs nuevas reciben 429. */
    private static final int MAX_KEYS = 10_000;
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Límite de un endpoint con sus ventanas por IP. Todas duran lo mismo y cada ventana nueva va al final, así que
     * el mapa queda en orden de vencimiento: las vencidas se borran desde el inicio sin recorrerlo todo.
     */
    private static final class Rule {
        final int limit;
        final Duration duration;
        final Map<String, Window> windows = new LinkedHashMap<>();

        Rule(int limit, Duration duration) {
            this.limit = limit;
            this.duration = duration;
        }
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
    private final TrustedProxies trustedProxies;

    @Autowired
    public RateLimitFilter(
            @Value("${fatfish.security.rate-limit.enabled:false}") boolean enabled,
            @Value("${fatfish.security.rate-limit.register-per-hour:5}") int registerPerHour,
            @Value("${fatfish.security.rate-limit.login-per-minute:10}") int loginPerMinute,
            @Value("${fatfish.security.rate-limit.feedback-per-minute:10}") int feedbackPerMinute,
            @Value("${fatfish.security.rate-limit.survey-per-minute:10}") int surveyPerMinute,
            @Value("${fatfish.security.rate-limit.trusted-proxies:}") String trustedProxies) {
        this(enabled, registerPerHour, loginPerMinute, feedbackPerMinute, surveyPerMinute, Clock.systemUTC(),
                TrustedProxies.parse(trustedProxies.isBlank() ? TrustedProxies.GOOGLE : trustedProxies));
    }

    RateLimitFilter(boolean enabled, int registerPerHour, int loginPerMinute, int feedbackPerMinute,
            int surveyPerMinute, Clock clock) {
        this(enabled, registerPerHour, loginPerMinute, feedbackPerMinute, surveyPerMinute, clock,
                TrustedProxies.parse(TrustedProxies.GOOGLE));
    }

    private RateLimitFilter(boolean enabled, int registerPerHour, int loginPerMinute, int feedbackPerMinute,
            int surveyPerMinute, Clock clock, TrustedProxies trustedProxies) {
        this.enabled = enabled;
        this.clock = clock;
        this.trustedProxies = trustedProxies;
        this.rules = Map.of(
                "/api/juego/registro", new Rule(registerPerHour, Duration.ofHours(1)),
                "/api/juego/sesion", new Rule(loginPerMinute, Duration.ofMinutes(1)),
                "/api/pwa/retroalimentacion", new Rule(feedbackPerMinute, Duration.ofMinutes(1)),
                "/api/pwa/encuesta", new Rule(surveyPerMinute, Duration.ofMinutes(1)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !rules.containsKey(path(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = path(request);
        long retryAfter = consume(rules.get(path), clientIp(request));
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
    private synchronized long consume(Rule rule, String ip) {
        Instant now = clock.instant();
        removeExpired(rule, now);
        Window window = rule.windows.get(ip);
        if (window == null) {
            if (rule.windows.size() >= MAX_KEYS) {
                // Lleno de ventanas activas: no se descarta ninguna, la IP nueva espera a que venza la más antigua
                return secondsUntil(now, rule.windows.values().iterator().next().end);
            }
            window = new Window(now.plus(rule.duration));
            rule.windows.put(ip, window);
        }
        if (window.count < rule.limit) {
            window.count++;
            return 0;
        }
        return secondsUntil(now, window.end);
    }

    private static void removeExpired(Rule rule, Instant now) {
        Iterator<Window> iterator = rule.windows.values().iterator();
        while (iterator.hasNext()) {
            if (now.isBefore(iterator.next().end)) {
                return;
            }
            iterator.remove();
        }
    }

    private static long secondsUntil(Instant now, Instant end) {
        return Math.max(1, (Duration.between(now, end).toMillis() + 999) / 1000);
    }

    /**
     * Ruta ya decodificada y sin parámetros de ruta (;x=1), igual a la que usa Spring para elegir el controlador.
     * getRequestURI() no sirve: llega sin decodificar y X-Forwarded-Prefix le agrega un prefijo.
     */
    private static String path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return request.getServletPath() + (pathInfo == null ? "" : pathInfo);
    }

    /**
     * Cloud Run agrega al final de X-Forwarded-For la IP que se le conecta; lo que va antes lo puede escribir el
     * cliente. Cuando la petición llega por Firebase Hosting, esa última IP es de un proxy de Google y la del jugador
     * va justo antes. Por eso se recorre la cabecera desde el final y se toma la primera IP que no sea de un proxy de
     * confianza. Se lee de la petición original porque en el perfil cloudrun ForwardedHeaderFilter oculta la
     * cabecera y deja en getRemoteAddr() la primera IP, que es falsificable.
     */
    String clientIp(HttpServletRequest request) {
        ServletRequest original = request;
        while (original instanceof ServletRequestWrapper wrapper) {
            original = wrapper.getRequest();
        }
        String forwarded = ((HttpServletRequest) original).getHeader(FORWARDED_FOR);
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        String[] ips = forwarded.split(",");
        int index = ips.length - 1;
        while (index > 0 && trustedProxies.contains(ips[index].strip())) {
            index--;
        }
        if (index < ips.length - 1) {
            // Para confirmar en Cloud Run qué manda Firebase: LOGGING_LEVEL_COM_FATFISH_API_CONFIG_RATELIMITFILTER=DEBUG
            log.debug("X-Forwarded-For con proxy de confianza: {} -> {}", forwarded, ips[index].strip());
        }
        return ips[index].strip();
    }
}
