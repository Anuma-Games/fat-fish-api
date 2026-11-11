package com.fatfish.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fatfish.api.MutableClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private final MutableClock clock = new MutableClock();
    // 3 fallos seguidos bloquean 15 minutos
    private final LoginAttemptService service = new LoginAttemptService(true, 3, 15, clock);

    @Test
    void blocksAfterTheConfiguredFailuresAndReleasesWhenTheTimeEnds() {
        service.recordFailure(1);
        service.recordFailure(1);
        assertThat(service.isBlocked(1)).isFalse();

        service.recordFailure(1);
        assertThat(service.isBlocked(1)).isTrue();

        clock.advance(Duration.ofMinutes(14));
        assertThat(service.isBlocked(1)).isTrue();

        clock.advance(Duration.ofMinutes(2));
        assertThat(service.isBlocked(1)).isFalse();
    }

    @Test
    void aSuccessfulLoginRestartsTheCount() {
        service.recordFailure(1);
        service.recordFailure(1);
        service.reset(1);
        service.recordFailure(1);
        service.recordFailure(1);

        assertThat(service.isBlocked(1)).isFalse();
    }

    @Test
    void countsEachPlayerSeparately() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(1);
        }

        assertThat(service.isBlocked(1)).isTrue();
        assertThat(service.isBlocked(2)).isFalse();
    }

    @Test
    void failuresOutsideTheWindowDoNotAddUp() {
        service.recordFailure(1);
        service.recordFailure(1);
        clock.advance(Duration.ofMinutes(16));
        service.recordFailure(1);

        assertThat(service.isBlocked(1)).isFalse();
    }

    @Test
    void manyOtherIdsDoNotRemoveAnActiveBlock() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure(1);
        }

        for (long id = 2; id < 20_000; id++) {
            service.recordFailure(id);
        }

        assertThat(service.isBlocked(1)).isTrue();
    }

    @Test
    void neverBlocksWhenDisabled() {
        LoginAttemptService disabled = new LoginAttemptService(false, 3, 15, clock);

        for (int i = 0; i < 10; i++) {
            disabled.recordFailure(1);
        }

        assertThat(disabled.isBlocked(1)).isFalse();
    }
}
