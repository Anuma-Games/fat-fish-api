package com.fatfish.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fatfish.api.MutableClock;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.ForwardedHeaderFilter;

class RateLimitFilterTest {

    /** Servlet que responde 200 a todo, para que la cadena llegue al final. */
    private static final HttpServlet OK = new HttpServlet() {
        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) {
        }
    };

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
    void usesTheLastIpOfXForwardedForSoASpoofedFirstIpDoesNotHelp() throws Exception {
        for (int i = 0; i < 3; i++) {
            send(filter, "POST", "/api/juego/sesion", "10.0.0." + i + ", 9.9.9.9");
        }

        assertThat(send(filter, "POST", "/api/juego/sesion", "10.0.0.99, 9.9.9.9").getStatus()).isEqualTo(429);
    }

    @Test
    void countsAPlayerBehindFirebaseHostingByTheirOwnIp() throws Exception {
        // Firebase Hosting reenvía desde una IP de Google distinta en cada petición; la del jugador va antes
        send(filter, "POST", "/api/juego/sesion", "203.0.113.5, 74.125.212.68");
        send(filter, "POST", "/api/juego/sesion", "203.0.113.5, 64.233.172.9");
        send(filter, "POST", "/api/juego/sesion", "203.0.113.5, 192.178.15.104");

        assertThat(send(filter, "POST", "/api/juego/sesion", "203.0.113.5, 74.125.215.76").getStatus())
                .isEqualTo(429);
        assertThat(send(filter, "POST", "/api/juego/sesion", "198.51.100.7, 74.125.212.68").getStatus())
                .isEqualTo(200);
    }

    @Test
    void stillLimitsBehindForwardedHeaderFilterLikeTheCloudrunProfile() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(sendThroughForwardedHeaderFilter("10.0.0." + i + ", 9.9.9.9", null).getStatus())
                    .isEqualTo(200);
        }

        // Ni otra IP falsa al inicio ni un X-Forwarded-Prefix que cambie getRequestURI() evitan el límite
        assertThat(sendThroughForwardedHeaderFilter("10.0.0.99, 9.9.9.9", "/otro").getStatus()).isEqualTo(429);
    }

    @Test
    void keepsActiveWindowsWhenTheMapIsFull() throws Exception {
        for (int i = 0; i < 2; i++) {
            send(filter, "POST", "/api/juego/registro", "1.1.1.1");
        }
        for (int i = 1; i < 10_000; i++) {
            send(filter, "POST", "/api/juego/registro", "10." + (i / 65_536) + "." + (i / 256 % 256) + "." + (i % 256));
        }

        // Ninguna ventana activa se descarta: la IP que ya gastó su límite sigue en 429 y una IP nueva espera
        assertThat(send(filter, "POST", "/api/juego/registro", "1.1.1.1").getStatus()).isEqualTo(429);
        assertThat(send(filter, "POST", "/api/juego/registro", "2.2.2.2").getStatus()).isEqualTo(429);

        clock.advance(Duration.ofHours(1));

        assertThat(send(filter, "POST", "/api/juego/registro", "2.2.2.2").getStatus()).isEqualTo(200);
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
        MockHttpServletRequest request = request(method, path);
        request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse sendThroughForwardedHeaderFilter(String forwardedFor, String prefix)
            throws Exception {
        MockHttpServletRequest request = request("POST", "/api/juego/sesion");
        request.addHeader("X-Forwarded-For", forwardedFor);
        if (prefix != null) {
            request.addHeader("X-Forwarded-Prefix", prefix);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MockFilterChain(OK, new ForwardedHeaderFilter(), filter).doFilter(request, response);
        return response;
    }

    /** Como Tomcat con el DispatcherServlet en "/": la ruta completa queda en getServletPath(). */
    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
