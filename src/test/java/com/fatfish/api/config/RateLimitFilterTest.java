package com.fatfish.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fatfish.api.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private final MutableClock clock = new MutableClock();
    // registro 2 por hora; sesion, retroalimentacion y encuesta 3 por minuto
    private final RateLimitFilter filter = new RateLimitFilter(true, 2, 3, 3, 3, clock);

    @Test
    void answers429OnRequestNPlusOneWithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(send(filter, "POST", "/api/juego/sesion", "1.1.1.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = send(filter, "POST", "/api/juego/sesion", "1.1.1.1");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentType()).startsWith("application/problem+json");
    }

    @Test
    void startsANewWindowWhenTheTimeEnds() throws Exception {
        for (int i = 0; i < 3; i++) {
            send(filter, "POST", "/api/pwa/encuesta", "1.1.1.1");
        }
        assertThat(send(filter, "POST", "/api/pwa/encuesta", "1.1.1.1").getStatus()).isEqualTo(429);

        clock.advance(Duration.ofSeconds(61));

        assertThat(send(filter, "POST", "/api/pwa/encuesta", "1.1.1.1").getStatus()).isEqualTo(200);
    }

    @Test
    void limitsEachIpAndEachEndpointSeparately() throws Exception {
        for (int i = 0; i < 2; i++) {
            send(filter, "POST", "/api/juego/registro", "2.2.2.2");
        }

        assertThat(send(filter, "POST", "/api/juego/registro", "2.2.2.2").getStatus()).isEqualTo(429);
        assertThat(send(filter, "POST", "/api/juego/registro", "3.3.3.3").getStatus()).isEqualTo(200);
        assertThat(send(filter, "POST", "/api/pwa/retroalimentacion", "2.2.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void usesTheFirstIpOfXForwardedFor() throws Exception {
        for (int i = 0; i < 3; i++) {
            send(filter, "POST", "/api/juego/sesion", "9.9.9.9, 10.0.0.1");
        }

        assertThat(send(filter, "POST", "/api/juego/sesion", "9.9.9.9, 10.0.0.2").getStatus()).isEqualTo(429);
    }

    @Test
    void doesNotLimitMatchesOrReadRequests() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(send(filter, "POST", "/api/juego/partidas", "1.1.1.1").getStatus()).isEqualTo(200);
            assertThat(send(filter, "GET", "/api/pwa/perfil", "1.1.1.1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void doesNothingWhenDisabled() throws Exception {
        RateLimitFilter disabled = new RateLimitFilter(false, 1, 1, 1, 1, clock);

        for (int i = 0; i < 10; i++) {
            assertThat(send(disabled, "POST", "/api/juego/sesion", "1.1.1.1").getStatus()).isEqualTo(200);
        }
    }

    private static MockHttpServletResponse send(RateLimitFilter filter, String method, String path,
            String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}