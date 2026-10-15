package com.fatfish.api.dto.pwa;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Paginated response for the PWA (field names in Spanish, as in the PWA contract).
 * {@code generadoEn} lets the PWA show how old the data is when it comes from the offline cache.
 */
public record PwaPage<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean ultima,
        Instant generadoEn) {

    public static <S, T> PwaPage<T> from(Page<S> page, Function<S, T> mapper) {
        return new PwaPage<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                Instant.now());
    }
}
