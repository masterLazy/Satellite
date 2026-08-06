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

public class RegisterRepository {
    private static final String FILE_NAME = "register.json";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, RegisterEntry> entryMap = new LinkedHashMap<>();
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
        lock.readLock().lock();
        try{
            return entryMap.get(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasEntry(String username) {
        lock.readLock().lock();
        try{
            return entryMap.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void putEntry(RegisterEntry registerEntry) {
        lock.writeLock().lock();
        try {
            entryMap.put(registerEntry.name(),registerEntry);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public String[] getEntryNames() {
        lock.readLock().lock();
        try {
            return entryMap.keySet().toArray(new String[0]);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void save() {
        lock.readLock().lock();
        try {
            try (BufferedWriter writer = Files.newWriter(jsonFile, StandardCharsets.UTF_8)) {
                Satellite.GSON.toJson(entryMap.values(), writer);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to write {}", jsonFile, e);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void load() {
        lock.writeLock().lock();
        try {
            if (!jsonFile.exists()) {
                Satellite.LOGGER.warn("[Satellite] {} not found; will clear registration list in memory.", jsonFile);
                entryMap.clear();
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

            Map<String, RegisterEntry> newMap = new LinkedHashMap<>();
            for (RegisterEntry registerEntry : loaded) {
                String username = registerEntry.name();
                if (newMap.containsKey(username)) {
                    Satellite.LOGGER.warn("[Satellite] Duplicate username ignored: {}", username);
                    continue;
                }
                newMap.put(username, registerEntry);
            }
            entryMap.clear();
            entryMap.putAll(newMap);
            Satellite.LOGGER.info("[Satellite] Loaded {} ({} users)", jsonFile, entryMap.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
}
