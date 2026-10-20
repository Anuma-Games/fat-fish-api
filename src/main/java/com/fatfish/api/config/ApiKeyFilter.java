package com.fatfish.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Pide el encabezado X-Api-Key en /api/**. Sin llave configurada el filtro no hace nada (local y CI);
 * con api-key-required=true la aplicación no arranca si falta la llave, para no quedar abierta por error.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";

    private final byte[] expected;
    private static final List<String> OPEN_PREFIXES =List.of("/actuator/health", "/swagger-ui", "/v3/api-docs");

    public ApiKeyFilter(@Value("${fatfish.security.api-key:}") String key,
            @Value("${fatfish.security.api-key-required:false}") boolean required) {
        if (required && key.isBlank()) {
            throw new IllegalStateException("api-key-required=true pero fatfish.security.api-key (FATFISH_SECURITY_API_KEY) está vacía");
        }
        this.expected = key.strip().getBytes(StandardCharsets.UTF_8);
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (expected.length == 0) {
            return true;
        }
        String path = request.getRequestURI();
        return OPEN_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String sent = request.getHeader(HEADER);
        boolean valid = sent != null
                && MessageDigest.isEqual(sent.strip().getBytes(StandardCharsets.UTF_8), expected);
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"title\":\"Acceso denegado\",\"status\":403,"
                    + "\"detail\":\"Falta la llave de acceso o no es válida\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}