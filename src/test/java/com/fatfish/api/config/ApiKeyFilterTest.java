package com.fatfish.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyFilterTest {
     
    private final ApiKeyFilter filter = new ApiKeyFilter("llave-de-prueba", true);

    @Test
    void rejectsARequestWithoutKey() throws Exception {
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean passed = send(filter, "/api/pwa/perfil", null, response);

        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void rejectsAWrongKey() throws Exception {
        assertThat(send(filter, "/api/juego/registro", "otra", new MockHttpServletResponse())).isFalse();
    }

    @Test
    void acceptsTheRightKey() throws Exception {
        assertThat(send(filter, "/api/juego/registro", "llave-de-prueba", new MockHttpServletResponse())).isTrue();
    }

    @Test
    void doesNotProtectHealthChecks() throws Exception {
        assertThat(send(filter, "/actuator/health", null, new MockHttpServletResponse())).isTrue();
    }

    @Test
    void isDisabledWithoutAKeyWhenNotRequired() throws Exception {
        ApiKeyFilter disabled = new ApiKeyFilter("", false);
        assertThat(send(disabled, "/api/pwa/perfil", null, new MockHttpServletResponse())).isTrue();
    }

    @Test
    void failsToStartWhenRequiredButMissing() {
        assertThatThrownBy(() -> new ApiKeyFilter("", true)).isInstanceOf(IllegalStateException.class);
    }

    private static boolean send(ApiKeyFilter filter, String path, String key, MockHttpServletResponse response)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (key != null) {
            request.addHeader("X-Api-Key", key);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain.getRequest() != null;
    }
}