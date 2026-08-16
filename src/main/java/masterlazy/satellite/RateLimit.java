package masterlazy.satellite;

import java.time.Duration;
import java.time.Instant;

public class RateLimit {
    private final long rateLimit;
    private final Duration rateReset;

    private long rate = 0;
    private Instant resetAt = Instant.now();

    public RateLimit(long rateLimit, Duration rateReset) {
        this.rateLimit = rateLimit;
        this.rateReset = rateReset;
    }

    public boolean tryAcquire() {
        Instant now = Instant.now();
        if (resetAt.isBefore(now)) {
            resetAt = now.plus(rateReset);
            rate = 0;
        }
        if (rate >= rateLimit) {
            return false;
        }
        rate++;
        return true;
    }

    // For "limit failure rate only"
    public void revertRate() {
        if (rate > 0) rate--;
    }
}
