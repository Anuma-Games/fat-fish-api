package com.fatfish.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TrustedProxiesTest {

    private final TrustedProxies google = TrustedProxies.parse(TrustedProxies.GOOGLE);

    @Test
    void recognizesTheGoogleProxiesSeenInCloudRunLogs() {
        assertThat(google.contains("74.125.212.68")).isTrue();
        assertThat(google.contains("64.233.172.9")).isTrue();
        assertThat(google.contains("192.178.15.104")).isTrue();
        assertThat(google.contains("2607:f8b0:4005:80f::200e")).isTrue();
    }

    @Test
    void checksTheWholePrefix() {
        // 192.178.0.0/15 cubre 192.178.x.x y 192.179.x.x
        assertThat(google.contains("192.179.255.255")).isTrue();
        assertThat(google.contains("192.180.0.0")).isFalse();
    }

    @Test
    void rejectsBadRangesWhenTheApplicationStarts() {
        for (String bad : new String[] {"1.2.3.0/33", "2001:4860::/129", "1.2.3.0", "1.2.3.0/x", "proxy/8", "1.2.3.0/8/"}) {
            assertThatThrownBy(() -> TrustedProxies.parse(bad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(bad);
        }
    }

    @Test
    void doesNotTrustPlayersOrAnythingThatIsNotAnIp() {
        assertThat(google.contains("187.189.58.228")).isFalse();
        assertThat(google.contains("2806:2f0:34e0:750c:45b:c718:15ca:99c0")).isFalse();
        assertThat(google.contains("999.1.1.1")).isFalse();
        assertThat(google.contains("proxy.example.com")).isFalse();
        assertThat(google.contains("")).isFalse();
    }
}
