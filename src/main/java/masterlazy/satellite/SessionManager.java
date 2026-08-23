package masterlazy.satellite;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SessionManager <S extends HasUuid> {
    protected Map<UUID, S> sessionMap = new ConcurrentHashMap<>();

    public @Nullable S get(UUID uuid) {
        return sessionMap.get(uuid);
    }

    public void register(S session) {
        sessionMap.put(session.getUUID(), session);
    }

    public void expire(S session) {
        sessionMap.remove(session.getUUID());
    }
}
