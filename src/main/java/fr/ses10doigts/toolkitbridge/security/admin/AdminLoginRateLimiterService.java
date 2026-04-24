package fr.ses10doigts.toolkitbridge.security.admin;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AdminLoginRateLimiterService {

    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentMap<String, AttemptState> attemptsByIp = new ConcurrentHashMap<>();
    private final Clock clock;

    public AdminLoginRateLimiterService() {
        this(Clock.systemUTC());
    }

    AdminLoginRateLimiterService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public boolean isBlocked(String ipAddress) {
        purgeExpiredStates();
        String key = normalizeIp(ipAddress);
        AttemptState state = attemptsByIp.get(key);
        if (state == null) {
            return false;
        }
        return state.isBlockedAt(Instant.now(clock));
    }

    public void recordFailure(String ipAddress) {
        purgeExpiredStates();
        String key = normalizeIp(ipAddress);
        Instant now = Instant.now(clock);
        attemptsByIp.compute(key, (ignored, current) -> {
            AttemptState base = current;
            if (base == null || base.isWindowExpiredAt(now)) {
                base = AttemptState.firstFailure(now);
            } else if (base.isBlockedAt(now)) {
                return base;
            } else {
                base = base.withAdditionalFailure();
            }

            if (base.failureCount >= MAX_FAILURES) {
                return base.withBlockedUntil(now.plus(BLOCK_DURATION));
            }
            return base;
        });
    }

    public void reset(String ipAddress) {
        purgeExpiredStates();
        attemptsByIp.remove(normalizeIp(ipAddress));
    }

    private void purgeExpiredStates() {
        Instant now = Instant.now(clock);
        attemptsByIp.entrySet().removeIf(entry -> entry.getValue().canBePurgedAt(now));
    }

    private String normalizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }
        return ipAddress.trim();
    }

    private static final class AttemptState {
        private final int failureCount;
        private final Instant firstFailureAt;
        private final Instant blockedUntil;

        private AttemptState(int failureCount, Instant firstFailureAt, Instant blockedUntil) {
            this.failureCount = failureCount;
            this.firstFailureAt = firstFailureAt;
            this.blockedUntil = blockedUntil;
        }

        private static AttemptState firstFailure(Instant now) {
            return new AttemptState(1, now, null);
        }

        private AttemptState withAdditionalFailure() {
            return new AttemptState(failureCount + 1, firstFailureAt, blockedUntil);
        }

        private AttemptState withBlockedUntil(Instant blockedUntil) {
            return new AttemptState(failureCount, firstFailureAt, blockedUntil);
        }

        private boolean isBlockedAt(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        private boolean isWindowExpiredAt(Instant now) {
            return firstFailureAt.plus(FAILURE_WINDOW).isBefore(now);
        }

        private boolean canBePurgedAt(Instant now) {
            return !isBlockedAt(now) && isWindowExpiredAt(now);
        }
    }
}
