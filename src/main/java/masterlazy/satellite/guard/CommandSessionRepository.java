package masterlazy.satellite.guard;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.guard.model.CommandSession;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class CommandSessionRepository {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    // We won't have many sessions, I think... so not PriorityQueue here
    private final HashMap<UUID, CommandSession> sessionMap = new HashMap<>();

    private Instant nextCheck = Instant.now();

    public void register() {
        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            Instant now = Instant.now();
            if (nextCheck.isAfter(now)) return;
            else nextCheck = now.plus(Duration.ofSeconds(1));
            sessionMap.entrySet().removeIf(entry -> {
                CommandSession session = entry.getValue();
                if (session.expireAt().isBefore(now)) {
                    ServerPlayer player = Satellite.Server.getPlayerList().getPlayer(session.caller());
                    if (player != null) {
                        Satellite.sendMessageWithKey(player, "guard.cmd.expiredYours", session.command());
                    }
                    return true;
                }
                return false;
            });
        });
    }

    public void addSession(CommandSession session) {
        withWriteLock(() -> sessionMap.put(session.uuid(), session));
    }

    public void expireSession(CommandSession session) {
        withWriteLock(() -> sessionMap.remove(session.uuid()));
    }

    @Nullable
    public CommandSession getSession(UUID uuid) {
        return withReadLock(() -> sessionMap.get(uuid));
    }

    @Nullable
    public CommandSession getSession(ServerPlayer caller, String command) {
        return withReadLock(() -> {
            for (CommandSession session : sessionMap.values()) {
                if (session.caller() == caller.getUUID() && session.command().equals(command)) {
                    return session;
                }
            }
            return null;
        });
    }

    // Helpers

    private <T> T withReadLock(Supplier<T> task) {
        lock.readLock().lock();
        try {
            return task.get();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when reading CommandSessionRepository", e);
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    private void withWriteLock(Runnable task) {
        lock.writeLock().lock();
        try {
            task.run();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to CommandSessionRepository", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
