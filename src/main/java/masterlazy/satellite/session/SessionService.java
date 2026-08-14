package masterlazy.satellite.session;

import masterlazy.satellite.session.handler.EventHandler;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionService {
    private final EventHandler eventHandler;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final HashMap<UUID, PlayerSession> sessions = new HashMap<>();

    public static final int REQUEST_RATE_LIMIT = 1200;
    public static final Duration REQUEST_RATE_RESET = Duration.ofSeconds(60);

    public static final int AUTHORIZE_RATE_LIMIT = 5;
    public static final Duration AUTHORIZE_RATE_RESET = Duration.ofSeconds(60);

    public SessionService() {
        eventHandler = new EventHandler(this);
    }

    public void onInitialize() {
        eventHandler.register();
    }

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
