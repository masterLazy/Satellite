package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.payload.ConsoleFeedS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO: 增加定时清理无效会话逻辑
public class SubscribeManager {
    private final TokenRepository tokenRepository;
    private final HashMap<String, Instant> subscribers = new HashMap<>(); // <token, expireAt>
    // Watch latest.log
    private static final String FILE_PATH = "logs/latest.log";
    private static final String FILE_NAME = "latest.log";
    private Path logFile;
    private WatchService watchService;
    private String logContent;
    private boolean remoteConsoleAvailable = false;
    // Feed & fetch
    private UUID lastFeedId = UUID.randomUUID();
    private int last1000LineAt = -1;

    public static final Duration TIMEOUT_SUBSCRIBE = Duration.ofMinutes(5);

    public SubscribeManager(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public void onInitialize() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            logFile = Paths.get(FILE_PATH);
            logFile.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to create watch service. Remote console is disabled", e);
            return;
        } catch (InvalidPathException e) {
            Satellite.LOGGER.error("[Satellite] Failed to locate {}. Remote console is disabled", FILE_PATH, e);
            return;
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Failed to register watcher for {}. Remote console is disabled", FILE_PATH, e);
            return;
        }
        // Read
        try {
            logContent = Files.readString(logFile);
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to read {}. Remote console is disabled", FILE_PATH, e);
            return;
        }
        // In background thread
        ExecutorService watcherThreadPool = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(this::watcherLoop, watcherThreadPool);
        remoteConsoleAvailable = true;
    }

    public synchronized void subscribe(String token) {
        subscribers.put(token, (Instant.now().plus(TIMEOUT_SUBSCRIBE)));
    }

    public synchronized boolean unsubscribe(String token) {
        return subscribers.remove(token) != null;
    }

    public synchronized void sendFeed(String content) {
        expireSubscribers();
        UUID feedId = UUID.randomUUID();
        ConsoleFeedS2CPayload payload = new ConsoleFeedS2CPayload(feedId.toString(), lastFeedId.toString(), content);
        for (Map.Entry<String, Instant> e : subscribers.entrySet()) {
            ServerPlayer player =  Satellite.Server.getPlayerList().getPlayerByName(tokenRepository.getOwner(e.getKey()));
            if (player != null) ServerPlayNetworking.send(player, payload);
        }
        lastFeedId = feedId;
    }

    private synchronized void expireSubscribers() {
        Instant now = Instant.now();
        subscribers.entrySet().removeIf( entry -> {
            String token = entry.getKey();
            if (!tokenRepository.isTokenValid(token)) {
                return true;
            }
            if (Satellite.Server.getPlayerList().getPlayerByName(tokenRepository.getOwner(token)) == null) {
                return true;
            }
            return entry.getValue().isBefore(now);
        });
    }

    private void watcherLoop() {
        while (true) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path context = (Path) event.context();
                    if (!context.toString().equals(FILE_NAME)) continue;
                    last1000LineAt = -1;
                    String newContent = Files.readString(logFile);
                    if (newContent.startsWith(logContent)) {
                        sendFeed(newContent.substring(logContent.length()));
                        logContent = newContent;
                    } else {
                        lastFeedId = UUID.randomUUID(); // Let client re-fetch contents
                        sendFeed("");
                    }
                }
                Thread.sleep(100);
                // Path not accessible
                if (!key.reset()) break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                Satellite.LOGGER.error("[Satellite] Failed to read {}", FILE_PATH, e);
            }
        }
    }

    public synchronized String getLast1000Lines() {
        if (last1000LineAt == -1) {
            int count = 0;
            for (int i = logContent.length() - 1; i >= 0; i--) {
                if (logContent.charAt(i) != '\n') continue;
                count++;
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
