package masterlazy.satellite.remote;

import masterlazy.satellite.RateLimit;

import java.time.Duration;
import java.time.Instant;

public class RemoteSession {
    private final String token;
    private final String owner;

    private final RateLimit requestLimit = new RateLimit(1200, Duration.ofSeconds(60));
    private static final Duration TIMEOUT_INACTIVITY = Duration.ofMinutes(30);

    private Instant expireAt;

    public RemoteSession(String owner) {
        token = RemoteUtils.generateToken();
        this.owner = owner;
        refresh();
    }

    public String getToken() {
        return token;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isExpiredWhen(Instant now) {
        return expireAt.isBefore(now);
    }

    private void refresh() {
        expireAt = Instant.now().plus(TIMEOUT_INACTIVITY);
    }

    public boolean tryRequest() {
        if (requestLimit.tryAcquire()) {
            refresh();
            return true;
        }
        return false;
    }
}
