package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.remote.handler.CommandHandler;
import masterlazy.satellite.remote.handler.EventHandler;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;

public class RemoteService {
    private final RemoteSessionManager remoteSessionManager;
    private final FeedManager feedManager;

    private final CommandHandler commandHandler;
    private final EventHandler eventHandler;

    public final String VERSION = "v1";

    public RemoteService(AuthService authService) {
        remoteSessionManager = new RemoteSessionManager();
        feedManager = new FeedManager(remoteSessionManager);
        commandHandler = new CommandHandler(this, authService, feedManager);
        eventHandler = new EventHandler(this);
    }

    public void onInitialize() {
        remoteSessionManager.onInitialize();
        feedManager.onInitialize();
        eventHandler.register();
        // Hello
        PayloadTypeRegistry.playC2S().register(HelloC2SPayload.ID, HelloC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HelloS2CPayload.ID, HelloS2CPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HelloC2SPayload.ID, this::handleHello);

        // Command
        PayloadTypeRegistry.playC2S().register(CommandC2SPayload.ID, CommandC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CommandS2CPayload.ID, CommandS2CPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CommandC2SPayload.ID, commandHandler::handleCommandC2S);

        // ConsoleFeed
        PayloadTypeRegistry.playS2C().register(ConsoleFeedS2CPayload.ID, ConsoleFeedS2CPayload.CODEC);
    }

    public Status verifyRequest(String token, Context context) {
        RemoteSession session = remoteSessionManager.getValid(token);
        if (session == null) {
            return Status.UNAUTHORIZED;
        }
        if (!session.getOwner().equals(context.player().getName().getString()) || // Token not belong to sender
            !context.player().hasPermissions(3)) {
            return Status.FORBIDDEN;
        }
        if (!session.tryRequest()) {
            return Status.TOO_MANY_REQUEST;
        }
        return Status.OK;
    }

    public String getTokenFor(String owner) {
        return remoteSessionManager.registerFor(owner).getToken();
    }

    private void handleHello(HelloC2SPayload payload, Context context) {
        Satellite.B_LOGGER.debug("%s >> HelloC2SPayload:\n%s", context.player().getName().getString(), Satellite.GSON.toJson(payload));
        if (payload.isCompatible()) {
            Satellite.LOGGER.info("[Satellite] {} connected with a compatible Satellite client", context.player().getName().getString());
        }
    }
}
