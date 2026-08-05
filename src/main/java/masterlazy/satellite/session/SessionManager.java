package masterlazy.satellite.session;

import masterlazy.satellite.session.model.PlayerSession;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionManager {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final HashMap<UUID, PlayerSession> sessions = new HashMap<>();

    public final SessionHandler handler = new SessionHandler(this);

    @Nullable
    public PlayerSession getSession(ServerPlayer player) {
        lock.readLock().lock();
        try {
            return sessions.get(player.getUUID());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void registerSession(ServerPlayer player) {
        lock.writeLock().lock();
        try {
            sessions.put(player.getUUID(), new PlayerSession(player));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unregisterSession(ServerPlayer player) {
        lock.writeLock().lock();
        try {
            sessions.remove(player.getUUID());
        } finally {
            lock.writeLock().unlock();
        }
    }
}
