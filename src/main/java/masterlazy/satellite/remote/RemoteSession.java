package masterlazy.satellite.remote;

import masterlazy.satellite.RateLimit;
import masterlazy.satellite.Satellite;

import java.time.Duration;
import java.time.Instant;

public class RemoteSession {
    private final String token;
    private final String owner;

    private final RateLimit rateLimit = new RateLimit(Satellite.config.remote_requestLimitPerMinute(), Duration.ofSeconds(60));

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
        expireAt = Instant.now().plus(Duration.ofMinutes(Satellite.config.remote_sessionInactivityTimeoutMinutes()));
    }

    public boolean tryRequest() {
        if (rateLimit.tryAcquire()) {
            refresh();
            return true;
        }
        return false;
    }

    public int getTryAfterSecond() {
        return rateLimit.getTryAfterSeconds();
    }

}
