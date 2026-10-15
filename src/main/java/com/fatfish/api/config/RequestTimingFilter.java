package com.fatfish.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Test lab only (profile "lab"): logs one line per API request with method, path, status,
 * processing time, payload sizes and JVM heap usage.
 */
@Component
@Profile("lab")
public class RequestTimingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("fatfish.lab.http");
    private static final long MB = 1024 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        long start = System.nanoTime();
        try {
            chain.doFilter(request, wrapper);
        } finally {
            double millis = (System.nanoTime() - start) / 1_000_000.0;
            int responseBytes = wrapper.getContentSize();
            wrapper.copyBodyToResponse();
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / MB;
            String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
            log.info("{} {}{} -> {} | {} ms | petición {} B, respuesta {} B | heap {} MB de {} MB (máx. {} MB)",
                    request.getMethod(), request.getRequestURI(), query, wrapper.getStatus(),
                    String.format("%.1f", millis), Math.max(request.getContentLengthLong(), 0), responseBytes,
                    usedMb, runtime.totalMemory() / MB, runtime.maxMemory() / MB);
        }
    }
}
