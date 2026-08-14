package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.remote.handler.ConsoleCmdHandler;
import masterlazy.satellite.remote.handler.EventHandler;
import masterlazy.satellite.remote.model.RequestResult;
import masterlazy.satellite.remote.payload.*;
import masterlazy.satellite.session.PlayerSession;
import masterlazy.satellite.session.SessionService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;

public class RemoteService {
    private final SessionService sessionService;
    private final AuthService authService;

    private final TokenRepository tokenRepository;
    private final SubscribeManager subscribeManager;

    private final ConsoleCmdHandler consoleCmdHandler;
    private final EventHandler eventHandler;

    public final String VERSION = "v1";

    public RemoteService(SessionService sessionService, AuthService authService) {
        this.authService = authService;
        this.sessionService = sessionService;
        tokenRepository = new TokenRepository();
        subscribeManager = new SubscribeManager(tokenRepository);
        consoleCmdHandler = new ConsoleCmdHandler(this, subscribeManager);
        eventHandler = new EventHandler(this);
    }

    public void onInitialize() {
        subscribeManager.onInitialize();
        // Payloads
        PayloadTypeRegistry.playC2S().register(HelloC2SPayload.ID, HelloC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HelloS2CPayload.ID, HelloS2CPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(AuthorizeC2SPayload.ID, AuthorizeC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AuthorizeS2CPayload.ID, AuthorizeS2CPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(ConsoleCmdC2SPayload.ID, ConsoleCmdC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConsoleCmdS2CPayload.ID, ConsoleCmdS2CPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(ConsoleFeedS2CPayload.ID, ConsoleFeedS2CPayload.CODEC);

        // Handlers
        ServerPlayNetworking.registerGlobalReceiver(HelloC2SPayload.ID, this::handleHello);
        ServerPlayNetworking.registerGlobalReceiver(AuthorizeC2SPayload.ID, this::handleAuthorize);
        ServerPlayNetworking.registerGlobalReceiver(ConsoleCmdC2SPayload.ID, consoleCmdHandler::handleConsoleCmdC2S);
        eventHandler.register();
    }

    public RequestResult verifyRequest(String token, Context context) {
        PlayerSession session = sessionService.getSession(context.player());
        if (session == null) {
            return RequestResult.CONFLICT;
        }
        if (!session.tryRequest()) {
            return RequestResult.TOO_MANY_REQUEST;
        }
        if (!tokenRepository.isTokenValid(token)) {
            return RequestResult.UNAUTHORIZED;
        }
        return RequestResult.OK;
    }

    private void handleHello(HelloC2SPayload payload, Context context) {
        if (payload.isCompatible()) {
            Satellite.LOGGER.info("[Satellite] {} connected with a compatible Satellite client", context.player().getName().getString());
        }
    }

    private void handleAuthorize(AuthorizeC2SPayload payload, Context context) {
        PlayerSession session = sessionService.getSession(context.player());
        int id = payload.requestId();
        if (session == null) {
            ServerPlayNetworking.send(context.player(), new AuthorizeS2CPayload(id, RequestResult.CONFLICT.name(), ""));
            return;
        }
        if (!session.tryAuthorize()) {
            ServerPlayNetworking.send(context.player(), new AuthorizeS2CPayload(id, RequestResult.TOO_MANY_REQUEST.name(), ""));
            return;
        }
        String username = context.player().getName().getString();
        if (!authService.isCorrectPassword(username, payload.password())) {
            ServerPlayNetworking.send(context.player(), new AuthorizeS2CPayload(id, RequestResult.UNAUTHORIZED.name(), ""));
            return;
        }
        String token = tokenRepository.getNewToken(username);
        if (token == null) {
            ServerPlayNetworking.send(context.player(), new AuthorizeS2CPayload(id, RequestResult.INTERNAL_SERVER_ERROR.name(), ""));
            return;
        }
        ServerPlayNetworking.send(context.player(), new AuthorizeS2CPayload(id, RequestResult.OK.toString(), token));
        Satellite.LOGGER.info("Delivering token {} to {}",token, username);
    }
}
