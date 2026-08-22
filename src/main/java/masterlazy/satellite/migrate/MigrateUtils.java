package masterlazy.satellite.migrate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import masterlazy.satellite.migrate.model.Name;
import masterlazy.satellite.migrate.model.PlayerData;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class MigrateUtils {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern JOIN_MSG = Pattern.compile(".*?([a-zA-Z0-9_]+) joined the game");

    public static void parseFile(Map<UUID, PlayerData> map, Path path) {
        if (!Files.exists(path)) {
            System.out.println("\033[31mFailed to locate "+path+"\033[0m");
            return;
        }
        if (Files.isDirectory(path)) {
            System.out.println("\033[31m"+path+" is a directory\033[0m");
            return;
        }
        String filename = path.getFileName().toString();
        if (filename.endsWith(".json")) {
            parseJson(map, path);
        } else if (filename.endsWith(".log.gz"))  {
            parseLogGz(map, path);
        } else if (filename.endsWith(".log")) {
            parseLog(map, path);
        }
    }

    public static void parseLogDir(Map<UUID, PlayerData> map, Path path) {
        if (!Files.exists(path)) {
            System.out.println("\033[31mFailed to locate "+path+"\033[0m");
            return;
        }
        if (!Files.isDirectory(path)) {
            System.out.println("\033[31m"+path+" is a file\033[0m");
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> parseFile(map, p));
        } catch (IOException e) {
            System.out.println("\033[31mFailed to parse "+path+": "+e+"\033[0m");
        }
    }

    private static void parseJson(Map<UUID, PlayerData> map, Path path) {
        Name[] loaded;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            loaded = GSON.fromJson(reader, Name[].class);
        } catch (Exception e) {
            System.out.println("\033[31mFailed to parse "+path+": "+e+"\033[0m");
            return;
        }
        String[] names = new String[loaded.length];
        for (int i = 0; i < loaded.length; i++) {
            names[i] = loaded[i].name();
        }
        resolve(map, names, path.toString(), null);
    }

    private static void parseLogGz(Map<UUID, PlayerData> map, Path path) {
        var names = new HashSet<String>();
        try (FileInputStream fis = new FileInputStream(path.toString());
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader br = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = JOIN_MSG.matcher(line);
                if (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        } catch (Exception e) {
            System.out.println("\033[31mFailed to parse "+path+": "+e+"\033[0m");
            return;
        }
        resolve(map, names.toArray(String[]::new), path.toString(), parseDateOnly(path.getFileName().toString()));
    }

    private static void parseLog(Map<UUID, PlayerData> map, Path path) {
        var names = new HashSet<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toString(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = JOIN_MSG.matcher(line);
                if (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
        } catch (Exception e) {
            System.out.println("\033[31mFailed to parse "+path+": "+e+"\033[0m");
            return;
        }
        resolve(map, names.toArray(String[]::new), path.toString(), parseDateOnly(path.getFileName().toString()));
    }

    private static void resolve(Map<UUID, PlayerData> map, String[] names, String source, @Nullable LocalDate date) {
        var resolved = new AtomicInteger(0);
        var _new = new AtomicInteger(0);
        var same = new AtomicInteger(0);
        var conflict = new AtomicInteger(0);
        for (var name : names) {
            // Offline
            UUID id0 = MigrateUtils.getOfflineUUID(name);
            map.computeIfPresent(id0, (k, v) -> {
                if (v.name() == null) {
                    _new.getAndIncrement();
                } else if (v.name().equalsIgnoreCase(name)) {
                    same.getAndIncrement();
                } else {
                    System.out.println("Conflict: "+v.uuid()+" was "+v.name()+" but it can also be offline UUID of "+ name);
                    conflict.getAndIncrement();
                }
                resolved.getAndIncrement();
                return PlayerData.merge(new PlayerData(id0, name, date), v);
            });
            // Online
            UUID id = UUIDRepository.getUUID(name);
            map.computeIfPresent(id, (k, v) -> {
                if (v.name() == null) {
                    _new.getAndIncrement();
                } else if (v.name().equalsIgnoreCase(name)) {
                    same.getAndIncrement();
                } else {
                    System.out.println("Conflict: "+id+" was "+v.name()+" but it can also be online UUID of "+ name);
                    conflict.getAndIncrement();
                }
                resolved.getAndIncrement();
                return PlayerData.merge(new PlayerData(id, name, date), v);
            });
        }
        System.out.printf("Resolved %4d, \033[36m%4d\033[0m new, %4d same, \033[31m%4d\033[0m conflict from %s\r\n",
                resolved.get(), _new.get(), same.get(), conflict.get(), source);
    }

    public static UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static void mergeFromDir(Map<UUID, PlayerData> map, Path path, String ext) {
        try (Stream<Path> stream = Files.walk(path)) {
            var players = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(ext))
                    .map(p -> new PlayerData(UUID.fromString(removeExt(p.getFileName().toString())), null, null))
                    .toList();
            for (var player : players) {
                if (!map.containsKey(player.uuid())) {
                    map.put(player.uuid(), player);
                } else {
                    map.put(player.uuid(), PlayerData.merge(player, map.get(player.uuid())));
                }
            }
        } catch (IOException e) {
            System.out.println("\033[31mFailed to read "+path+": "+e+"\033[0m");
        }
    }

    private static String removeExt(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            return filename.substring(0, dot);
        }
        return filename;
    }

    private static @Nullable LocalDate parseDateOnly(String fileName) {
        try {
            // 1. 去除扩展名（.log 或 .log.gz）
            String base = fileName.replace(".log.gz", "").replace(".log", "");
            // 此时 base = "2026-02-16-1"

            // 2. 去掉最后的 "-序号"，只取日期部分 "2026-02-16"
            // 注意：找最后一个 '-'，因为日期里也有 '-'
            int lastDashIndex = base.lastIndexOf('-');
            if (lastDashIndex < 0) return null;
            String datePart = base.substring(0, lastDashIndex); // "2026-02-16"

            // 3. 解析为 LocalDate，然后转为 LocalDateTime（默认午夜 00:00:00）
            return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
