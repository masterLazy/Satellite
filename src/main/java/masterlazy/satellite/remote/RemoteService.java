package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.remote.handler.EventHandler;
import masterlazy.satellite.remote.model.Request;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.*;
import masterlazy.satellite.remote.pipeline.CommandHandler;
import masterlazy.satellite.remote.pipeline.FileHandler;
import masterlazy.satellite.remote.pipeline.HelloHandler;
import masterlazy.satellite.remote.pipeline.PayloadHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RemoteService {
    public static final String VERSION = "v1";

    private final AuthService authService;
    private final RemoteSessionManager remoteSessionManager;
    private final FeedManager feedManager;
    private final FileSessionManager fileSessionManager;

    private final EventHandler eventHandler;

    private final Map<CustomPacketPayload.Type<?>, PayloadHandler<? extends CustomPacketPayload>> handlers = new HashMap<>();

    public RemoteService(AuthService authService) {
        remoteSessionManager = new RemoteSessionManager();
        feedManager = new FeedManager(remoteSessionManager);
        fileSessionManager = new FileSessionManager();
        eventHandler = new EventHandler();
        this.authService = authService;
    }

    public void onInitialize() {
        remoteSessionManager.onInitialize();
        feedManager.onInitialize();
        fileSessionManager.onInitialize();
        eventHandler.register();
        // Hello
        PayloadTypeRegistry.playS2C().register(HelloS2CPayload.ID, HelloS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HelloC2SPayload.ID, HelloC2SPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HelloC2SPayload.ID, this::dispatcher);
        // Command
        PayloadTypeRegistry.playS2C().register(CommandS2CPayload.ID, CommandS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CommandC2SPayload.ID, CommandC2SPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CommandC2SPayload.ID, this::dispatcher);
        // Console Feed
        PayloadTypeRegistry.playS2C().register(ConsoleFeedS2CPayload.ID, ConsoleFeedS2CPayload.CODEC);
        // File
        PayloadTypeRegistry.playS2C().register(FileS2CPayload.ID, FileS2CPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FileC2SPayload.ID, FileC2SPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FileC2SPayload.ID, this::dispatcher);
        // Handlers
        handlers.put(HelloC2SPayload.ID, new HelloHandler());
        handlers.put(CommandC2SPayload.ID, new CommandHandler(this, authService, feedManager));
        handlers.put(FileC2SPayload.ID, new FileHandler(this, remoteSessionManager));
        Satellite.LOGGER.info("[Satellite] Initialized Remote module");
    }

    @SuppressWarnings("unchecked")
    private <PayloadT extends CustomPacketPayload> void dispatcher(PayloadT payload, Context ctx) {
        PayloadHandler<PayloadT> handler = (PayloadHandler<PayloadT>) handlers.get(payload.type());
        if (handler == null) {
            Satellite.LOGGER.error("[Satellite] Can't find handler for '{}' payload", payload.type());
        } else {
            CompletableFuture.runAsync(() -> handler.handle(new Request<>(payload, ctx)));
        }
    }

    public boolean verifyToken(Request<? extends HasToken> request, Consumer<Status> respond) {
        String token = request.payload().token();
        RemoteSession session = remoteSessionManager.getValid(token);
        if (session == null) {
            respond.accept(Status.UNAUTHORIZED);
            return true;
        }
        if (!session.getOwner().equals(request.sender())) { // Token not belong to sender
            respond.accept(Status.FORBIDDEN);
            Satellite.LOGGER.warn("[Satellite] {} offered a token that doesn't belong to they!", request.sender());
            return true;
        }
        if (!session.tryRequest()) {
            respond.accept(Status.TOO_MANY_REQUEST);
            return true;
        }
        return false;
    }

    public @Nullable String getTokenFor(String owner) {
        RemoteSession session = remoteSessionManager.registerFor(owner);
        if (session == null) return null;
        return session.getToken();
    }

    public void putFileSession(FileSession session) {
        fileSessionManager.put(session);
    }

    public @Nullable FileSession getFileSession(UUID id) {
        return fileSessionManager.getValid(id);
    }
}
