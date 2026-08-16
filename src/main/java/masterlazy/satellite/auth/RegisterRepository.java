package masterlazy.satellite.auth;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.WithReadWriteLock;
import masterlazy.satellite.auth.model.RegisterEntry;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegisterRepository extends WithReadWriteLock {
    @Override
    protected String getClassName() { return RegisterRepository.class.getName(); }

    private static final String FILE_NAME = "register.json";
    private final Map<String, RegisterEntry> registerMap = new LinkedHashMap<>();
    private final Path jsonFile;

    public RegisterRepository(String baseDir) {
        jsonFile = Paths.get(baseDir, FILE_NAME);
        if (Files.exists(jsonFile)) {
            load();
        } else {
            save();
            Satellite.LOGGER.warn("[Satellite] {} not found; creating a new one.", jsonFile);
        }
    }

    @Nullable
    public RegisterEntry getEntry(String username) {
        return withReadLock(() -> registerMap.get(username)).orElse(null);
    }

    public boolean hasEntry(String username) {
        return withReadLock(() -> registerMap.containsKey(username)).orElse(false);
    }

    @Nullable
    public String[] getEntryNames() {
        return withReadLock(() -> registerMap.keySet().toArray(new String[0])).orElse(null);
    }

    public void putEntry(RegisterEntry registerEntry) {
        withWriteLock(() -> registerMap.put(registerEntry.name(), registerEntry));
    }

    public void save() {
        withWriteLock(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(jsonFile, StandardCharsets.UTF_8)) {
                Satellite.GSON.toJson(registerMap.values(), writer);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to write {}", jsonFile, e);
            }
        });
    }

    public void load() {
        withWriteLock(() -> {
            if (Files.notExists(jsonFile)) {
                Satellite.LOGGER.warn("[Satellite] {} not found; will clear registration list in memory.", jsonFile);
                registerMap.clear();
                return;
            }

            RegisterEntry[] loaded;
            try (BufferedReader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
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
}
