package masterlazy.satellite;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public abstract class SessionManager <S extends HasUuid> extends WithReadWriteLock {
    protected HashMap<UUID, S> sessionMap = new HashMap<>();

    @Nullable
    public S get(UUID uuid) {
        return withReadLock(() -> sessionMap.get(uuid)).orElse(null);
    }

    public void register(S session) {
        withWriteLock(() -> sessionMap.put(session.getUUID(), session));
    }

    public void expire(S session) {
        withWriteLock(() -> sessionMap.remove(session.getUUID()));
    }
}
