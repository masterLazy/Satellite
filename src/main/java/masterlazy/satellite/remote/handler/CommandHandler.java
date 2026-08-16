package masterlazy.satellite.remote.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.auth.AuthSession;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.FeedManager;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandC2SPayload;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import org.jetbrains.annotations.Nullable;


public class CommandHandler {
    private final RemoteService service;
    private final AuthService authService;
    private final FeedManager feedManager;

    public CommandHandler(RemoteService service, AuthService authService, FeedManager feedManager) {
        this.service = service;
        this.authService = authService;
        this.feedManager = feedManager;
    }

    private static void sendS2C(CommandC2SPayload C2SPayload, Context context, Status status, @Nullable String[] results) {
        CommandS2CPayload feedback = new CommandS2CPayload(C2SPayload.requestId(), status, results == null ? new String[0] : results);
        ServerPlayNetworking.send(context.player(), feedback);
        Satellite.B_LOGGER.debug("%s << CommandS2CPayload:\n%s", context.player().getName().getString(), Satellite.GSON.toJson(feedback));
    }

    public void handleCommandC2S(CommandC2SPayload payload, Context context) {
        Satellite.B_LOGGER.debug("%s >> CommandC2SPayload:\n%s", context.player().getName().getString(), Satellite.GSON.toJson(payload));
        // Basic verification
        if (payload.command() == CommandEnum.AUTHORIZE) {
            handleAuthorize(payload, context);
            return;
        }
        Status status = service.verifyRequest(payload.token(), context);
        if (status != Status.OK) {
            sendS2C(payload, context, status, null);
            return;
        }
        // End of basic verification
        CommandEnum command = payload.command();
        if (command == CommandEnum.UNKNOWN) {
            sendS2C(payload, context, Status.BAD_REQUEST, null);
            return;
        }
        if (command == CommandEnum.SUBSCRIBE || command == CommandEnum.UNSUBSCRIBE || command == CommandEnum.FETCH_1000) {
            handleConsoleFeed(payload, context);
        } else if (command == CommandEnum.EXECUTE) {
            handleExecute(payload, context);
        }
    }

    public void handleAuthorize(CommandC2SPayload payload, Context context) {
        if (Satellite.isSingleGame()) { // Skip authorization
            String username = context.player().getName().getString();
            String token = service.getTokenFor(username);
            sendS2C(payload, context, Status.OK, new String[]{token});
            return;
        }
        // Basic verification
        AuthSession session = authService.getSession(context.player());
        if (session == null) {
            sendS2C(payload, context, Status.FORBIDDEN, null);
            return;
        }
        if (!session.tryAuthorize()) {
            sendS2C(payload, context, Status.TOO_MANY_REQUEST, null);
            return;
        }
        // End of basic verification
        if (payload.args().length < 1) {
            sendS2C(payload, context, Status.BAD_REQUEST, null);
            return;
        }
        if (!context.player().hasPermissions(3)) { // Op only
            sendS2C(payload, context, Status.FORBIDDEN, null);
            return;
        }
        String username = context.player().getName().getString();
        if (!authService.isCorrectPassword(username, payload.args()[0])) {
            sendS2C(payload, context, Status.UNAUTHORIZED, null);
            return;
        }
        String token = service.getTokenFor(username);
        if (token == null) {
            sendS2C(payload, context, Status.INTERNAL_SERVER_ERROR, null);
            return;
        }
        sendS2C(payload, context, Status.OK, new String[]{token});
    }

    public void handleConsoleFeed(CommandC2SPayload payload, Context context) {
        switch (payload.command()) {
            case SUBSCRIBE -> {
                if (feedManager.subscribe(payload.token())) {
                    sendS2C(payload, context, Status.OK, null);
                } else {
                    sendS2C(payload, context, Status.FORBIDDEN, null);
                }
            }
            case UNSUBSCRIBE -> {
                if (feedManager.unsubscribe(payload.token())) {
                    sendS2C(payload, context, Status.OK, null);
                } else {
                    sendS2C(payload, context, Status.FORBIDDEN, null);
                }
            }
            case FETCH_1000 -> {
                sendS2C(payload, context, Status.OK, new String[]{feedManager.getLast1000Lines()});
            }
        }
    }

    public void handleExecute(CommandC2SPayload payload, Context context) {
        if (payload.args().length < 1) {
            sendS2C(payload, context, Status.BAD_REQUEST, null);
            return;
        }
        Satellite.execute(payload.args()[0]);
        sendS2C(payload, context, Status.OK, null);
    }
}
