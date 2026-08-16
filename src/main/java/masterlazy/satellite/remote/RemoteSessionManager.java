package masterlazy.satellite.remote;

import masterlazy.satellite.WithReadWriteLock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

public class RemoteSessionManager extends WithReadWriteLock {
    @Override
    protected String getClassName() { return RemoteSessionManager.class.getName(); }

    private final HashMap<String, RemoteSession> sessionMap = new HashMap<>();

    private Instant nextCheck = Instant.now();
    private static final Duration CHECK_BETWEEN = Duration.ofSeconds(15);

    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Instant now = Instant.now();
            if (nextCheck.isAfter(now)) return;
            else nextCheck = now.plus(CHECK_BETWEEN);
            sessionMap.entrySet().removeIf(entry -> entry.getValue().isExpiredWhen(now));
        });
    }

    public RemoteSession registerFor(String owner) {
        RemoteSession session = new RemoteSession(owner);
        sessionMap.put(session.getToken(), session);
        return session;
    }

    @Nullable
    public RemoteSession getValid(String token) {
        return withWriteLock(() -> {
            RemoteSession session = sessionMap.get(token);
            if (session == null) {
                return null;
            }
            if (session.isExpiredWhen(Instant.now())) {
                sessionMap.remove(token);
            }
            return session;
        }).orElse(null);
    }
}
