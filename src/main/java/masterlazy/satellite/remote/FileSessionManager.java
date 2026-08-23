package masterlazy.satellite.remote;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileSessionManager {
    private final Map<UUID, FileSession> sessionMap = new ConcurrentHashMap<>();

    private Instant nextCheck = Instant.now();
    private static final Duration CHECK_BETWEEN = Duration.ofSeconds(10);

    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Instant now = Instant.now();
            if (nextCheck.isAfter(now)) return;
            else nextCheck = now.plus(CHECK_BETWEEN);
            sessionMap.entrySet().removeIf(entry -> entry.getValue().isExpiredWhen(now));
        });
    }

    public void put(FileSession session) {
        sessionMap.put(session.getId(), session);
    }

    @Nullable
    public FileSession getValid(UUID id) {
        FileSession session = sessionMap.get(id);
        if (session == null || session.isExpiredWhen(Instant.now())) {
            return null;
        }
        return session;
    }
}