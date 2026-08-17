package masterlazy.satellite.remote.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.auth.AuthSession;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.FeedManager;
import masterlazy.satellite.remote.RemoteUtils;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandC2SPayload;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


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
        if (payload.command() == CommandEnum.AUTHORIZE) {
            handleAuthorize(payload, context);
            return;
        }
        Status status = service.verifyRequest(payload.token(), context);
        if (status != Status.OK) {
            sendS2C(payload, context, status, null);
            return;
        }
        CommandEnum command = payload.command();
        if (command == CommandEnum.UNKNOWN) {
            sendS2C(payload, context, Status.BAD_REQUEST, null);
            return;
        }
        // Deliver to handlers
        if (command == CommandEnum.SUBSCRIBE || command == CommandEnum.UNSUBSCRIBE || command == CommandEnum.FETCH_1000) {
            handleConsoleFeed(payload, context);
        } else if (command == CommandEnum.EXECUTE) {
            handleExecute(payload, context);
        } else if (command == CommandEnum.LIST || command == CommandEnum.MOVE || command == CommandEnum.COPY || command == CommandEnum.REMOVE) {
            handleFileCommand(payload, context);
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
        if (!feedManager.isRemoteConsoleAvailable()) {
            sendS2C(payload, context, Status.INTERNAL_SERVER_ERROR, null);
            return;
        }
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

    public void handleFileCommand(CommandC2SPayload payload, Context context) {
        switch (payload.command()) {
            case LIST -> {
                if (payload.args().length < 1) {
                    sendS2C(payload, context, Status.BAD_REQUEST, null);
                    break;
                }
                Path path = verifyPath(payload.args()[0], payload, context);
                if (path == null) break;
                try (var stream = Files.list(path)) {
                    List<Path> allChildren = stream.toList();
                    List<String> subDirs = allChildren.stream()
                            .filter(Files::isDirectory)
                            .map(p -> p.getFileName().toString())
                            .toList();
                    List<String> subFiles = allChildren.stream()
                            .filter(Files::isRegularFile)
                            .map(p -> p.getFileName().toString())
                            .toList();
                    List<String> paths = new ArrayList<>();
                    paths.add(((Integer) subDirs.size()).toString());
                    paths.addAll(subDirs);
                    paths.addAll(subFiles);
                    sendS2C(payload, context, Status.OK, paths.toArray(String[]::new));
                } catch (IOException e) {
                    sendS2C(payload, context, Status.INTERNAL_SERVER_ERROR, null);
                }
            }
        }
    }

    @Nullable
    private Path verifyPath(String p, CommandC2SPayload payload, Context context) {
        if (!p.startsWith("/")) {
            sendS2C(payload, context, Status.NOT_FOUND, null);
            return null;
        }
        Path path = Paths.get(p.substring(1));
        if (!RemoteUtils.isSubDirectory(Paths.get(""), path) && !p.equals("/")) { // Not sub folder
            sendS2C(payload, context, Status.FORBIDDEN, null);
            return null;
        } else if (!Files.exists(path)) {
            sendS2C(payload, context, Status.NOT_FOUND, null);
            return null;
        }
        return path;
    }
}
