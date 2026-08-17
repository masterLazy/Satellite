package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.payload.ConsoleFeedS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

// TODO: 增加定时清理无效会话逻辑
public class FeedManager {
    private final RemoteSessionManager remoteSessionManager;
    private final HashMap<String, Instant> subscribers = new HashMap<>(); // <token, expireAt>
    // Watch latest.log
    private static final String FILE_PATH = "logs/latest.log";
    private Path logFile;
    private String logContent;
    private long lastSize = -1;
    private boolean remoteConsoleAvailable = false;
    // Feed & fetch
    private UUID lastFeedId = UUID.randomUUID();
    private int last1000LineAt = -1;

    public static final Duration TIMEOUT_SUBSCRIBE = Duration.ofMinutes(5);

    private static final int POLL_INTERVAL_MILLISECONDS = 500;
    private static final int MAX_LINE_LENGTH = 1024;
    private static final int MAX_LOG_LENGTH = 1024 * 1024;

    private final ScheduledExecutorService watcherScheduler = Executors.newSingleThreadScheduledExecutor();

    public FeedManager(RemoteSessionManager remoteSessionManager) {
        this.remoteSessionManager = remoteSessionManager;
    }

    public void onInitialize() {
        try {
            logFile = Paths.get(FILE_PATH);
            if (!Files.exists(logFile)) {
                Satellite.LOGGER.warn("[Satellite] {} not found. Remote console will start once the file is created.", FILE_PATH);
                // We'll read file later...
            } else {
                logContent = Files.readString(logFile);
                lastSize = Files.size(logFile); // Initialize lastSize
            }
        } catch (IOException | InvalidPathException e) {
            Satellite.LOGGER.error("[Satellite] Failed to read {}. Remote console is disabled", FILE_PATH, e);
            return;
        }

        watcherScheduler.scheduleAtFixedRate(
                this::watcherTick,
                0,
                POLL_INTERVAL_MILLISECONDS,
                TimeUnit.MILLISECONDS
        );
        Satellite.LOGGER.info("[Satellite] Log watcher scheduler started");
        remoteConsoleAvailable = true;
    }

    public synchronized boolean subscribe(String token) {
        RemoteSession session = remoteSessionManager.getValid(token);
        if (session == null) return false;
        subscribers.put(token, (Instant.now().plus(TIMEOUT_SUBSCRIBE)));
        Satellite.LOGGER.info("[Satellite] {} subscribed to console output stream", session.getOwner());
        return true;
    }

    public synchronized boolean unsubscribe(String token) {
        return subscribers.remove(token) != null;
    }

    public synchronized void sendFeed(String content) {
        expireSubscribers();
        UUID feedId = UUID.randomUUID();
        ConsoleFeedS2CPayload payload = new ConsoleFeedS2CPayload(feedId, lastFeedId, content);
        for (Map.Entry<String, Instant> e : subscribers.entrySet()) {
            RemoteSession session = remoteSessionManager.getValid(e.getKey());
            if (session == null) continue;
            ServerPlayer player =  Satellite.Server.getPlayerList().getPlayerByName(session.getOwner());
            if (player != null) {
                ServerPlayNetworking.send(player, payload);
                Satellite.B_LOGGER.debug("%s << ConsoleFeedS2CPayload:\n%s", player.getName().getString(), Satellite.GSON.toJson(payload));
            }
        }
        lastFeedId = feedId;
    }

    private synchronized void expireSubscribers() {
        Instant now = Instant.now();
        subscribers.entrySet().removeIf( entry -> {
            RemoteSession session = remoteSessionManager.getValid(entry.getKey());
            // Token expired / invalid
            if (session == null || Satellite.Server.getPlayerList().getPlayerByName(session.getOwner()) == null) {
                return true;
            }
            // Subscribe timeout
            return entry.getValue().isBefore(now);
        });
    }

    private void watcherTick() {
        try {
            // File not exists
            if (!Files.exists(logFile)) {
                return;
            }

            long size = Files.size(logFile);

            // First time, read but not feed
            if (lastSize < 0) {
                logContent = Files.readString(logFile);
                lastSize = size;
                return;
            }

            /* Factors to influence `size`:
             *   1. Appended new log
             *   2. Minecraft started a new log file when 23:59 -> 0:00
             *   3. Other edit to file (we can't prevent)
             */
            if (size != lastSize) {
                String appended;
                if (size > lastSize) {
                    appended = readFrom(logFile, lastSize, size);
                } else {
                    appended = Files.readString(logFile);
                }
                logContent += appended;
                if (logContent.length() > MAX_LOG_LENGTH) {
                    logContent = logContent.substring(logContent.length() - MAX_LOG_LENGTH);
                }
                lastSize = size;
                last1000LineAt = -1;
                sendFeed(appended);
            }
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to tail {}", FILE_PATH, e);
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when watching log; watcher is exiting {}", FILE_PATH, e);
            remoteConsoleAvailable = false;
        }
    }

    private String readFrom(Path path, long from, long to) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(from);
            int length = (int) (to - from);
            byte[] buffer = new byte[length];
            raf.readFully(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

    public synchronized String getLast1000Lines() {
        if (last1000LineAt == -1) {
            int count = 0;
            int lineLength = 0;
            for (int i = logContent.length() - 1; i >= 0; i--) {
                if (logContent.charAt(i) != '\n' && lineLength < MAX_LINE_LENGTH) {
                    lineLength++;
                    continue;
                }
                count++;
                lineLength = 0;
                if (count == 1000 + 1) {
                    last1000LineAt = i;
                    break;
                }
            }
            if (last1000LineAt == -1) last1000LineAt = 0;
        }
        return logContent.substring(last1000LineAt);
    }

    public boolean isRemoteConsoleAvailable() {
        return remoteConsoleAvailable;
    }
}
