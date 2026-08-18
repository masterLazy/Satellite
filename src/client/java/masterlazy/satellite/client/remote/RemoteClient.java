package masterlazy.satellite.client.remote;

import masterlazy.satellite.client.remote.cli.ShellContext;
import masterlazy.satellite.client.remote.command.SatelliteCommand;
import masterlazy.satellite.client.remote.cli.SshServer;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.payload.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteClient {
    private final SshServer sshServer = new SshServer();
    private boolean remoteAvailable = false;

    private final ResponseManager<CommandS2CPayload> commandResponseManager = new ResponseManager<>();
    private final BlockingQueue<ConsoleFeedS2CPayload> feedQueue = new LinkedBlockingQueue<>();

    public final int COMMAND_TIMEOUT_SECONDS = 10;
    public final int FEED_TIMEOUT_MILLISECONDS = 10;

    private final char[] spinner = {'/', '-', '\\', '|'};
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public boolean isRemoteAvailable() { return remoteAvailable; }

    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->{
            SatelliteCommand.register(dispatcher, this, sshServer);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client)->{
            sshServer.close();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            sshServer.close();
        });
        // Payloads
        ClientPlayNetworking.registerGlobalReceiver(HelloS2CPayload.ID, this::handleHelloS2C);
        ClientPlayNetworking.registerGlobalReceiver(CommandS2CPayload.ID, commandResponseManager::handle);
        ClientPlayNetworking.registerGlobalReceiver(ConsoleFeedS2CPayload.ID, this::handleConsoleFeedS2C);
    }

    private void handleHelloS2C(HelloS2CPayload payload, Context context) {
        if (ClientPlayNetworking.canSend(HelloC2SPayload.ID.id()) && payload.version().equals(RemoteService.VERSION)) {
            ClientPlayNetworking.send(new HelloC2SPayload(true));
            remoteAvailable = true;
        }
    }

    private void handleConsoleFeedS2C(ConsoleFeedS2CPayload payload, Context context) {
        feedQueue.offer(payload);
    }

    /**
     * Send CommandC2SPayload and wait for response
     * @return `null` if response timeout
     */
    @Nullable
    public CommandS2CPayload sendAndWait(ShellContext ctx, CommandEnum command, @Nullable String[] args) throws InterruptedException, ExecutionException {
        AtomicInteger index = new AtomicInteger(0);
        ScheduledFuture<?> animTask = scheduler.scheduleAtFixedRate(() -> {
            ctx.print("\r" + spinner[index.getAndIncrement() % spinner.length] + " ");
        }, 0, 100, TimeUnit.MILLISECONDS);
        UUID requestId = UUID.randomUUID();
        Future<CommandS2CPayload> future = commandResponseManager.responseFor(requestId);
        ClientPlayNetworking.send(new CommandC2SPayload(requestId, ctx.token(), command, args == null ? new String[0] : args));
        try {
            return future.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            animTask.cancel(true);
            ctx.println("\r\033[31mResponse timeout after "+COMMAND_TIMEOUT_SECONDS+"s\033[0m");
            future.cancel(true);
            return null;
        } finally {
            animTask.cancel(true);
            ctx.print("\r  \r");
        }
    }

    @Nullable
    public ConsoleFeedS2CPayload pollFeed() throws InterruptedException {
        return feedQueue.poll(FEED_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
    }
}
