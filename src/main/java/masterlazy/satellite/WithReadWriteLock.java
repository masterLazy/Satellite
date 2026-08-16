package masterlazy.satellite;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public abstract class WithReadWriteLock {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected abstract String getClassName();

    protected <T> Optional<T> withReadLock(Supplier<@Nullable T> task) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(task.get());
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when reading {}", getClassName(), e);
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    protected <T> Optional<T> withWriteLock(Supplier<@Nullable T> task) {
        lock.writeLock().lock();
        try {
            return Optional.ofNullable(task.get());
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to {}", getClassName(), e);
            return Optional.empty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void withWriteLock(Runnable task) {
        lock.writeLock().lock();
        try {
            task.run();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to {}",getClassName(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected boolean withWriteLockB(Supplier<@NotNull Boolean> task) {
        lock.writeLock().lock();
        try {
            return task.get();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to {}", getClassName(), e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
