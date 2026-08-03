package masterlazy.satellite.auth;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import masterlazy.satellite.Satellite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuthJson {

    private static final File JSON_FILE = new File("registered-players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    });

    private final Map<String, JsonObject> playerMap = new LinkedHashMap<>();
    private JsonArray jsonArray = new JsonArray();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public AuthJson() {
        read();
    }

    public boolean isRegistered(String username) {
        lock.readLock().lock();
        try {
            return playerMap.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isCorrectPassword(String username, String password) {
        lock.readLock().lock();
        try {
            JsonObject player = playerMap.get(username);
            if (player == null) return false;
            String storedHash = player.get("pwd_hash").getAsString();
            return storedHash.equals(sha256Hex(password));
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Add new password entry and write to file immediately */
    public void save(String username, String password) {
        lock.writeLock().lock();
        try {
            String key = username;
            JsonObject player = playerMap.get(key);
            String hash = sha256Hex(password);

            if (player != null) {
                player.addProperty("name", key);
                player.addProperty("pwd_hash", hash);
            } else {
                player = new JsonObject();
                player.addProperty("name", key);
                player.addProperty("pwd_hash", hash);
                jsonArray.add(player);
                playerMap.put(key, player);
            }
            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Read from file */
    public void read() {
        lock.writeLock().lock();
        try {
            if (!JSON_FILE.exists()) {
                Satellite.LOGGER.warn("[Satellite] {} not found, creating a new one.", JSON_FILE);
                return;
            }

            JsonArray loaded;
            try (BufferedReader reader = Files.newReader(JSON_FILE, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, JsonArray.class);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to parse {}. Keeping current data.", JSON_FILE, e);
                return;
            }

            if (loaded == null) {
                Satellite.LOGGER.error("[Satellite] {} is empty or invalid.", JSON_FILE);
                return;
            }

            Map<String, JsonObject> newMap = new LinkedHashMap<>();
            boolean changed = false;

            for (int i = 0; i < loaded.size(); i++) {
                JsonObject obj = loaded.get(i).getAsJsonObject();
                String username = obj.get("name").getAsString();

                if (newMap.containsKey(username)) {
                    Satellite.LOGGER.warn("[Satellite] Duplicate username ignored: {}", username);
                    changed = true;
                    continue;
                }

                newMap.put(username, obj);
            }

            if (changed) {
                jsonArray = new JsonArray();
                for (JsonObject obj : newMap.values()) {
                    jsonArray.add(obj);
                }
                persist();
            } else {
                jsonArray = loaded;
            }

            playerMap.clear();
            playerMap.putAll(newMap);
            Satellite.LOGGER.info("[Satellite] Loaded {} ({} users)", JSON_FILE, playerMap.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ArrayList<String> getPlayers() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(playerMap.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Write JSON to file */
    private void persist() {
        try (BufferedWriter writer = Files.newWriter(JSON_FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(jsonArray, writer);
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Failed to write {}", JSON_FILE, e);
        }
    }

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