package masterlazy.satellite.auth;

import com.google.common.io.Files;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.model.RegisterEntry;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class RegisterRepository {
    private static final String FILE_NAME = "register.json";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, RegisterEntry> registerMap = new LinkedHashMap<>();
    private final File jsonFile;

    public RegisterRepository(String baseDir) {
        jsonFile = new File(baseDir + FILE_NAME);
        if (jsonFile.exists()) {
            load();
        } else {
            save();
            Satellite.LOGGER.warn("[Satellite] {} not found; creating a new one.", jsonFile);
        }
    }

    @Nullable
    public RegisterEntry getEntry(String username) {
        return withReadLock(() -> registerMap.get(username));
    }

    public boolean hasEntry(String username) {
        return Boolean.TRUE.equals(withReadLock(() -> registerMap.containsKey(username)));
    }

    public String[] getEntryNames() {
        return withReadLock(() -> registerMap.keySet().toArray(new String[0]));
    }

    public void putEntry(RegisterEntry registerEntry) {
        withWriteLock(() -> registerMap.put(registerEntry.name(), registerEntry));
    }

    public void save() {
        withWriteLock(() -> {
            try (BufferedWriter writer = Files.newWriter(jsonFile, StandardCharsets.UTF_8)) {
                Satellite.GSON.toJson(registerMap.values(), writer);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to write {}", jsonFile, e);
            }
        });
    }

    public void load() {
        withWriteLock(() -> {
            if (!jsonFile.exists()) {
                Satellite.LOGGER.warn("[Satellite] {} not found; will clear registration list in memory.", jsonFile);
                registerMap.clear();
                return;
            }

            RegisterEntry[] loaded;
            try (BufferedReader reader = Files.newReader(jsonFile, StandardCharsets.UTF_8)) {
                loaded = Satellite.GSON.fromJson(reader, RegisterEntry[].class);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to parse {}. Keeping current data.", jsonFile, e);
                return;
            }
            if (loaded == null) {
                Satellite.LOGGER.error("[Satellite] {} is empty or invalid.", jsonFile);
                return;
            }

            boolean hasNull = false;
            for (RegisterEntry entry : loaded) {
                if (hasNullField(entry)) {
                    hasNull = true;
                    break;
                }
            }
            if (hasNull) {
                Satellite.LOGGER.warn("[Satellite] One or more registrations are incomplete in {}.", jsonFile);
            }

            Map<String, RegisterEntry> newMap = new LinkedHashMap<>();
            for (RegisterEntry registerEntry : loaded) {
                String username = registerEntry.name();
                if (newMap.containsKey(username)) {
                    Satellite.LOGGER.warn("[Satellite] Duplicate username ignored: {}", username);
                    continue;
                }
                newMap.put(username, registerEntry);
            }
            registerMap.clear();
            registerMap.putAll(newMap);
            Satellite.LOGGER.info("[Satellite] Loaded {} ({} users)", jsonFile, registerMap.size());
        });
    }

    public boolean hasNullField(RegisterEntry entry) {
        if (entry.name() == null) return true;
        if (entry.pwd_hash() == null) return true;
        return false;
    }

    // Helpers

    private <T> T withReadLock(Supplier<T> task) {
        lock.readLock().lock();
        try {
            return task.get();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when reading RegisterRepository", e);
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
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to RegisterRepository", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
