package fr.ses10doigts.toolkitbridge.security.admin;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLoginRateLimiterServiceTest {

    private static final String IP = "127.0.0.1";

    @Test
    void isNotBlockedInitially() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        AdminLoginRateLimiterService service = new AdminLoginRateLimiterService(clock);

        assertThat(service.isBlocked(IP)).isFalse();
    }

    @Test
    void blocksAfterFiveFailuresInWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        AdminLoginRateLimiterService service = new AdminLoginRateLimiterService(clock);

        for (int i = 0; i < 5; i++) {
            service.recordFailure(IP);
        }

        assertThat(service.isBlocked(IP)).isTrue();
    }

    @Test
    void blockExpiresAfterFifteenMinutes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        AdminLoginRateLimiterService service = new AdminLoginRateLimiterService(clock);
        for (int i = 0; i < 5; i++) {
            service.recordFailure(IP);
        }
        assertThat(service.isBlocked(IP)).isTrue();

        clock.setInstant(Instant.parse("2026-01-01T10:15:01Z"));

        assertThat(service.isBlocked(IP)).isFalse();
    }

    @Test
    void resetClearsStateForIp() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        AdminLoginRateLimiterService service = new AdminLoginRateLimiterService(clock);
        for (int i = 0; i < 5; i++) {
            service.recordFailure(IP);
        }
        assertThat(service.isBlocked(IP)).isTrue();

        service.reset(IP);

        assertThat(service.isBlocked(IP)).isFalse();
    }

    @Test
    void expiredStateBehavesLikeFreshFirstAttempt() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        AdminLoginRateLimiterService service = new AdminLoginRateLimiterService(clock);

        service.recordFailure(IP);
        clock.setInstant(Instant.parse("2026-01-01T10:11:00Z"));
        for (int i = 0; i < 4; i++) {
            service.recordFailure(IP);
        }
        assertThat(service.isBlocked(IP)).isFalse();

        service.recordFailure(IP);
        assertThat(service.isBlocked(IP)).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }
    }
}
