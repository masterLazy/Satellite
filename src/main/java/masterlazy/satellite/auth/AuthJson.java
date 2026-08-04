package masterlazy.satellite.auth;

import com.google.common.io.Files;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.dto.AuthEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuthJson {
    private static final String JSON_PATH = Satellite.BASE_DIR + "auth.json";
    private static final File JSON_FILE = new File(JSON_PATH);

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    });

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, AuthEntry> entryMap = new LinkedHashMap<>();

    public AuthJson() {
        if (JSON_FILE.exists()) {
            load();
        } else {
            Satellite.LOGGER.warn("[Satellite] {} not found; will create a new one.", JSON_FILE);
        }
    }

    public boolean isRegistered(String username) {
        lock.readLock().lock();
        try {
            return entryMap.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public String[] getRegisteredPlayers() {
        lock.readLock().lock();
        try {
            return entryMap.keySet().toArray(new String[0]);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isCorrectPassword(String username, String password) {
        lock.readLock().lock();
        try {
            AuthEntry entry = entryMap.get(username);
            if (entry == null) return false;
            return entry.pwd_hash().equals(sha256Hex(password));
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Save AuthEntry[] to file */
    public void save(String username, String password) {
        lock.writeLock().lock();
        try {
            AuthEntry entry = new AuthEntry(username, sha256Hex(password));
            entryMap.put(username, entry);

            try (BufferedWriter writer = Files.newWriter(JSON_FILE, StandardCharsets.UTF_8)) {
                Satellite.GSON.toJson(entryMap.values(), writer);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to write {}", JSON_FILE, e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load AuthEntry[] from file */
    public void load() {
        lock.writeLock().lock();
        try {
            if (!JSON_FILE.exists()) return;

            AuthEntry[] loaded;
            try (BufferedReader reader = Files.newReader(JSON_FILE, StandardCharsets.UTF_8)) {
                loaded = Satellite.GSON.fromJson(reader, AuthEntry[].class);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to parse {}. Keeping current data.", JSON_FILE, e);
                return;
            }
            if (loaded == null) {
                Satellite.LOGGER.error("[Satellite] {} is empty or invalid.", JSON_FILE);
                return;
            }

            Map<String, AuthEntry> newMap = new LinkedHashMap<>();
            for (AuthEntry authEntry : loaded) {
                String username = authEntry.name();
                if (newMap.containsKey(username)) {
                    Satellite.LOGGER.warn("[Satellite] Duplicate username ignored: {}", username);
                    continue;
                }
                newMap.put(username, authEntry);
            }
            entryMap.clear();
            entryMap.putAll(newMap);
            Satellite.LOGGER.info("[Satellite] Loaded {} ({} users)", JSON_FILE, entryMap.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Helpers

    private static String sha256Hex(String str) {
        MessageDigest md = SHA256.get();
        md.reset();
        byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}