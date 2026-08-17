package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.remote.handler.EventHandler;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.*;
import masterlazy.satellite.remote.pipeline.CommandHandler;
import masterlazy.satellite.remote.pipeline.HelloHandler;
import masterlazy.satellite.remote.pipeline.PayloadHandler;
import masterlazy.satellite.remote.model.Request;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RemoteService {
    public static final String VERSION = "v1";

    private final RemoteSessionManager remoteSessionManager;
    private final FeedManager feedManager;

    private final EventHandler eventHandler;

    private final Map<CustomPacketPayload.Type<?>, PayloadHandler<? extends CustomPacketPayload>> handlers = new HashMap<>();

    public RemoteService(AuthService authService) {
        remoteSessionManager = new RemoteSessionManager();
        feedManager = new FeedManager(remoteSessionManager);
        eventHandler = new EventHandler(this);
        // Handlers
        handlers.put(HelloC2SPayload.ID, new HelloHandler());
        handlers.put(CommandC2SPayload.ID, new CommandHandler(this, authService, feedManager));
    }

    public void onInitialize() {
        remoteSessionManager.onInitialize();
        feedManager.onInitialize();
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
    }

    @SuppressWarnings("unchecked")
    private <PayloadT extends CustomPacketPayload> void dispatcher(PayloadT payload, Context ctx) {
        PayloadHandler<PayloadT> handler = (PayloadHandler<PayloadT>) handlers.get(payload.type());
        if (handler == null) {
            Satellite.LOGGER.error("[Satellite] Can't find handler for '{}' payload", payload.type());
        } else {
            handler.handle(new Request<>(payload, ctx));
        }
    }

    public boolean verifyToken(Request<? extends HasToken> request, Consumer<Status> respond) {
        String token = request.payload().token();
        RemoteSession session = remoteSessionManager.getValid(token);
        if (session == null) {
            respond.accept(Status.UNAUTHORIZED);
            return true;
        }
        if (!session.getOwner().equals(request.sender())) {
            respond.accept(Status.FORBIDDEN);
            return true;
        }
        if (!session.tryRequest()) {
            respond.accept(Status.TOO_MANY_REQUEST);
            return true;
        }
        return false;
    }

    public String getTokenFor(String owner) {
        return remoteSessionManager.registerFor(owner).getToken();
    }
}
