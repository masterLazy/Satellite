package masterlazy.satellite.migrate;

import masterlazy.satellite.migrate.model.NameId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class UUIDRepository {
    private static final String API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String FILE_NAME = "cache-online-uuid.json";
    private static final int RETRY_LIMIT = 0;
    private static final int RETRY_MIN_GAP_MS = 5000;

    private static final Map<String, UUID> CACHE = new HashMap<>();
    private static final Set<String> ERROR_CACHE = new HashSet<>();

    /**
     * Look up UUID on api.mojang.com; return UUID.randomUUID() if failed
     */
    public static UUID getUUID(String name) {
        if (ERROR_CACHE.contains(name)) {
            return UUID.randomUUID();
        }
        int retry = 0, gap = 0;
        if (!CACHE.containsKey(name)) {
            URL url;
            try {
                url = new URL(API + name);
            } catch (MalformedURLException e) {
                System.out.println("\033[31mFailed to fetch UUID for " + name + ": " + e + "\033[0m");
                ERROR_CACHE.add(name);
                return UUID.randomUUID();
            }
            while (true) {
                try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
                    NameId res = MigrateUtils.GSON.fromJson(reader, NameId.class);
                    CACHE.put(name, UUID.fromString(formatUuid(res.id())));
                    break;
                } catch (FileNotFoundException ignored) {
                } catch (Exception e) {
                    if (retry == 0) {
                        System.out.println("\033[31mFailed to fetch UUID for " + name + ": " + e + "\033[0m");
                    } else {
                        System.out.println("\033[31mFailed to fetch UUID for " + name + " (retried " + retry + "): " + e + "\033[0m");
                    }
                    if (e.getMessage().contains("429")) {// TODO: 及其脆弱的判断
                        gap += RETRY_MIN_GAP_MS;
                        retry++;
                        if (retry > RETRY_LIMIT) break;
                        try {
                            Thread.sleep(gap);
                        } catch (InterruptedException ex) {
                            System.out.println("\033[31mFailed to fetch UUID for " + name + ": " + e + "\033[0m");
                            ERROR_CACHE.add(name);
                            return UUID.randomUUID();
                        }
                        continue; // Retry
                    }
                    ERROR_CACHE.add(name);
                    return UUID.randomUUID();
                }
            }
            if (retry >= RETRY_LIMIT) {
                //System.out.println("\033[31mFailed to fetch UUID after trying " + retry + " times\033[0m");
                ERROR_CACHE.add(name);
                return UUID.randomUUID();
            }
        }
        return CACHE.get(name);
    }

    // Convert to normal UUID
    public static String formatUuid(String trimmedUuid) {
        return trimmedUuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"
        );
    }

    public static void load() {
        Path jsonFile = Paths.get(FILE_NAME);
        if (!Files.exists(jsonFile)) return;
        NameId[] loaded;
        try (BufferedReader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            loaded = MigrateUtils.GSON.fromJson(reader, NameId[].class);
            if (loaded == null) {
                System.out.println("\033[31m" + jsonFile + "is empty or invalid\033[0m");
                return;
            }
            for (var i : loaded) {
                CACHE.put(i.name(), UUID.fromString(i.id()));
            }
        } catch (Exception e) {
            System.out.println("\033[31mFailed to parse " + jsonFile + ": " + e + "\033[0m");
        }
    }

    public static void save() {
        Path jsonFile = Paths.get(FILE_NAME);
        try (BufferedWriter writer = Files.newBufferedWriter(jsonFile,StandardCharsets.UTF_8)) {
            NameId[] data = new NameId[CACHE.size()];
            int i = 0;
            for (var entry : CACHE.entrySet()) {
                data[i] = new NameId(entry.getKey(), entry.getValue().toString());
                i++;
            }
            MigrateUtils.GSON.toJson(data, writer);
        } catch (Exception e) {
            System.out.println("\033[31mFailed to write " + jsonFile + ": " + e + "\033[0m");
        }
    }
}
