package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.model.TokenEntry;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

// TODO: 增加定时清理无效令牌逻辑
public class TokenRepository {
    public final Duration TIMEOUT_INACTIVITY = Duration.ofMinutes(30);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final HashMap<String, TokenEntry> tokenMap = new HashMap<>();

    @Nullable
    public String getNewToken(String username) {
        String token = RemoteUtils.generateToken();
        boolean res = withWriteLock(() -> {
            tokenMap.put(token, new TokenEntry(username, Instant.now().plus(TIMEOUT_INACTIVITY)));
        });
        if (res) return token;
        return null;
    }

    @Nullable
    public String getOwner(String token) {
        return withReadLock(() -> {
            if (!tokenMap.containsKey(token)) return null;
            return tokenMap.get(token).owner();
        });
    }

    public boolean refreshToken(String token) {
        return Boolean.TRUE.equals(withWriteLock(() -> {
            TokenEntry old = tokenMap.get(token);
            if (old == null) return false;
            tokenMap.put(token, new TokenEntry(old.owner(), Instant.now().plus(TIMEOUT_INACTIVITY)));
            return true;
        }));
    }

    public boolean isTokenValid(String token) {
        return Boolean.TRUE.equals(withWriteLock(() -> {
            TokenEntry entry = tokenMap.get(token);
            if (entry == null) return false;
            if (entry.expireAt().isBefore(Instant.now())) {
                tokenMap.remove(token);
                return false;
            }
            return true;
        }));
    }

    private <T> T withReadLock(Supplier<T> task) {
        lock.readLock().lock();
        try {
            return task.get();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when reading TokenRepository", e);
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    private <T> T withWriteLock(Supplier<T> task) {
        lock.writeLock().lock();
        try {
            return task.get();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to TokenRepository", e);
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    private boolean withWriteLock(Runnable task) {
        lock.writeLock().lock();
        try {
            task.run();
            return true;
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to TokenRepository", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
