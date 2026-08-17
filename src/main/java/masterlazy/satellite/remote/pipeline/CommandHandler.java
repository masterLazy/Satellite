package masterlazy.satellite.remote.pipeline;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.auth.AuthSession;
import masterlazy.satellite.remote.FeedManager;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.RemoteUtils;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Request;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandC2SPayload;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class CommandHandler implements PayloadHandler<CommandC2SPayload> {
    private final Pipeline<CommandC2SPayload> pipeline = new Pipeline<>();

    private final RemoteService service;
    private final AuthService authService;
    private final FeedManager feedManager;

    public CommandHandler(RemoteService service, AuthService authService, FeedManager feedManager) {
        this.service = service;
        this.authService = authService;
        this.feedManager = feedManager;
        pipeline.add(this::handleSingleGame)
                .add(this::verifyPermission)
                .add(this::verifyCommandEnum)
                .add(this::handleAuthorize)
                .add(request -> service.verifyToken(request, s -> respond(request, s, null)))
                .add(this::handleConsoleFeed)
                .add(this::handleExecute)
                .add(this::handleList);
    }

    @Override
    public boolean handle(Request<CommandC2SPayload> request) {
        Satellite.B_LOGGER.debug("%s >> CommandS2CPayload:\n%s", request.sender(), Satellite.GSON.toJson(request.payload()));
        return pipeline.handle(request);
    }

    private static boolean respond(Request<CommandC2SPayload> request, Status status, @Nullable String[] results) {
        CommandS2CPayload feedback = new CommandS2CPayload(request.payload().requestId(), status, results == null ? new String[0] : results);
        request.respond(feedback);
        Satellite.B_LOGGER.debug("%s << CommandS2CPayload:\n%s", request.sender(), Satellite.GSON.toJson(feedback));
        return true;
    }

    // Common

    private boolean verifyCommandEnum(Request<CommandC2SPayload> request) {
        if (request.payload().command() == CommandEnum.UNKNOWN) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        return false;
    }

    private boolean verifyPermission(Request<CommandC2SPayload> request) {
        if (!request.player().hasPermissions(3)) { // Op only
            return respond(request, Status.FORBIDDEN, null);
        }
        return false;
    }

    // Dedicated

    private boolean handleSingleGame(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (!Satellite.isSingleGame()) return false;
        if (payload.command() == CommandEnum.AUTHORIZE) {
            String token = service.getTokenFor(request.sender());
            return respond(request, Status.OK, new String[]{token});
        }
        return false;
    }

    private boolean handleAuthorize(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (payload.command() != CommandEnum.AUTHORIZE) return false;
        AuthSession session = authService.getSession(request.player());
        if (session == null) {
            return respond(request, Status.FORBIDDEN, null);
        }
        if (!session.tryAuthorize()) {
            return respond(request, Status.TOO_MANY_REQUEST, null);
        }
        String[] args = payload.args();
        if (args.length < 1) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        if (!authService.isCorrectPassword(request.sender(), args[0])) {
            return respond(request, Status.UNAUTHORIZED, null);
        }
        String token = service.getTokenFor(request.sender());
        if (token == null) {
            return respond(request, Status.INTERNAL_SERVER_ERROR, null);
        }
        respond(request, Status.OK, new String[]{token});
        return true;
    }

    public boolean handleConsoleFeed(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (payload.command() != CommandEnum.SUBSCRIBE &&
            payload.command() != CommandEnum.UNSUBSCRIBE &&
            payload.command() != CommandEnum.FETCH_1000) return false;

        if (!feedManager.isRemoteConsoleAvailable()) {
            return respond(request, Status.INTERNAL_SERVER_ERROR, null);
        }
        switch (payload.command()) {
            case SUBSCRIBE -> {
                if (feedManager.subscribe(payload.token())) {
                    respond(request, Status.OK, null);
                } else {
                    respond(request, Status.FORBIDDEN, null);
                }
            }
            case UNSUBSCRIBE -> {
                if (feedManager.unsubscribe(payload.token())) {
                    respond(request, Status.OK, null);
                } else {
                    respond(request, Status.FORBIDDEN, null);
                }
            }
            case FETCH_1000 -> {
                respond(request, Status.OK, new String[]{feedManager.getLast1000Lines()});
            }
        }
        return true;
    }

    public boolean handleExecute(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (payload.command() != CommandEnum.EXECUTE) return false;
        String[] args = payload.args();
        if (args.length < 1) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        Satellite.execute(args[0]);
        return respond(request, Status.OK, null);
    }

    public boolean handleList(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (payload.command() != CommandEnum.LIST) return false;
        String[] args = payload.args();
        if (args.length < 1) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        Path path = verifyPath(args[0], request);
        if (path == null || !Files.isDirectory(path)) {
            return respond(request, Status.NOT_FOUND, null);
        }
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
            return respond(request, Status.OK, paths.toArray(String[]::new));
        } catch (IOException e) {
            return respond(request, Status.INTERNAL_SERVER_ERROR, null);
        }
    }

    // Helpers

    @Nullable
    private Path verifyPath(String p, Request<CommandC2SPayload> request) {
        if (!p.startsWith("/")) {
            respond(request, Status.NOT_FOUND, null);
            return null;
        }
        Path path = Paths.get(p.substring(1));
        if (!RemoteUtils.isSubDirectory(Paths.get(""), path) && !p.equals("/")) { // Not sub folder
            respond(request, Status.FORBIDDEN, null);
            return null;
        } else if (!Files.exists(path)) {
            respond(request, Status.NOT_FOUND, null);
            return null;
        }
        return path;
    }
}
