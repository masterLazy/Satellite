package masterlazy.satellite.client.remote;

import masterlazy.satellite.Config;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.client.remote.cli.ConsoleCLI;
import masterlazy.satellite.client.remote.cli.ShellContext;
import masterlazy.satellite.client.remote.cli.SshServer;
import masterlazy.satellite.client.remote.command.SatelliteCommand;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.payload.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteClient {
    private final SshServer sshServer = new SshServer();
    private boolean remoteAvailable = false;

    public static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration GAME_ALIVE_CHECK_BETWEEN = Duration.ofMillis(100);

    public static final Duration FEED_OFFER_TIMEOUT = Duration.ofMillis(10);
    public static final Duration FEED_POLL_TIMEOUT = Duration.ofMillis(10);
    public static final int MAX_FEED_QUEUE_SIZE = 1024;

    public static final Duration FILE_OFFER_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration FILE_POLL_TIMEOUT = Duration.ofSeconds(5);
    public static final int MAX_FILE_QUEUE_SIZE = 1024;

    public static Config config;

    private final ResponseManager<CommandS2CPayload> commandResponseManager = new ResponseManager<>();
    private final BlockingQueue<ConsoleFeedS2CPayload> feedQueue = new LinkedBlockingQueue<>(MAX_FEED_QUEUE_SIZE);
    private final ConcurrentHashMap<UUID, BlockingQueue<FileS2CPayload>> fileQueues = new ConcurrentHashMap<>();

    private final char[] spinner = {'/', '-', '\\', '|'};
    private final ScheduledExecutorService animationScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService exitScheduler = Executors.newSingleThreadScheduledExecutor();

    public boolean isRemoteAvailable() { return remoteAvailable; }

    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> SatelliteCommand.register(dispatcher, this, sshServer));
        // Payloads
        ClientPlayNetworking.registerGlobalReceiver(HelloS2CPayload.ID, this::handleHelloS2C);
        ClientPlayNetworking.registerGlobalReceiver(CommandS2CPayload.ID, commandResponseManager::handle);
        ClientPlayNetworking.registerGlobalReceiver(ConsoleFeedS2CPayload.ID, this::handleConsoleFeedS2C);
        ClientPlayNetworking.registerGlobalReceiver(FileS2CPayload.ID, this::handleFileS2C);

        // TODO: I don't know why these two work well in dev client but don't work in formal client
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> shutdown());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
        // So I added this to make sure ssh server is closed when leaving game
        exitScheduler.scheduleAtFixedRate(
                () -> { if (!SatelliteClient.isInGame()) shutdown(); },
                0,
                GAME_ALIVE_CHECK_BETWEEN.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void shutdown() {
        if (!sshServer.isRunning()) return;
        sshServer.close();
        remoteAvailable = false;
        ConsoleCLI.isRunning = false;
    }

    private void handleHelloS2C(HelloS2CPayload payload, Context context) {
        Config config = payload.config();
        if (ClientPlayNetworking.canSend(HelloC2SPayload.ID.id())) {
            if (payload.version().equals(RemoteService.VERSION)) {
                ClientPlayNetworking.send(new HelloC2SPayload(true));
                remoteAvailable = true;
            }
            if (config.version() == new Config().version()) {
                Satellite.LOGGER.warn("[Satellite Client] Server sent a version-unmatched config (version={})", config.version());
            }
            RemoteClient.config = config;
        }
    }

    private void handleConsoleFeedS2C(ConsoleFeedS2CPayload payload, Context context) {
        try {
            if (feedQueue.offer(payload, FEED_OFFER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                return;
            }
        } catch (Exception ignored) {}
        Satellite.LOGGER.error("[Satellite Client] Failed to offer feedQueue");
    }

    private void handleFileS2C(FileS2CPayload payload, Context context) {
        try {
            if (getFileQueueFor(payload.sessionId()).offer(payload, FILE_OFFER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                return;
            }
        } catch (Exception ignored) {}
        Satellite.LOGGER.error("[Satellite Client] Failed to offer fileQueue (sessionId={})", payload.sessionId());
    }

    public BlockingQueue<FileS2CPayload> getFileQueueFor(UUID sessionId) {
        return fileQueues.computeIfAbsent(sessionId, k -> new LinkedBlockingQueue<>(MAX_FILE_QUEUE_SIZE));
    }

    /**
     * Send CommandC2SPayload and wait for response
     * @return `null` if response timeout
     */
    public @Nullable CommandS2CPayload sendAndWait(ShellContext ctx, CommandEnum command, @Nullable String[] args) throws InterruptedException, ExecutionException {
        AtomicInteger index = new AtomicInteger(0);
        ScheduledFuture<?> animTask = animationScheduler.scheduleAtFixedRate(() -> ctx.print("\r" + spinner[index.getAndIncrement() % spinner.length] + " "),
                0, 100, TimeUnit.MILLISECONDS);
        UUID requestId = UUID.randomUUID();
        Future<CommandS2CPayload> future = commandResponseManager.responseFor(requestId);
        ClientPlayNetworking.send(new CommandC2SPayload(requestId, ctx.token(), command, args == null ? new String[0] : args));
        try {
            return future.get(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            animTask.cancel(true);
            ctx.println("\r\033[31mResponse timeout after "+COMMAND_TIMEOUT.toMillis()+"ms\033[0m");
            future.cancel(true);
            return null;
        } finally {
            animTask.cancel(true);
            ctx.print("\r  \r");
        }
    }

    public @Nullable ConsoleFeedS2CPayload pollFeed() throws InterruptedException {
        return feedQueue.poll(FEED_POLL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }
}
