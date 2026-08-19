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
import org.apache.commons.io.file.PathUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
                .add(this::handleList)
                .add(this::handleMoveCopy)
                .add(this::handleRemove)
                .add(this::handleMkdirTouch);
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
            case FETCH_1000 -> respond(request, Status.OK, new String[]{feedManager.getLast1000Lines()});
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
        if (args.length < 2) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        Path path = getVerifiedPath(args[0]);
        if (path == null || !Files.exists(path) || !Files.isDirectory(path)) {
            return respond(request, Status.NOT_FOUND, null);
        }
        String options = args[1];
        if (options == null) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        boolean detailed = options.contains("l");
        try (var stream = Files.list(path)) {
            List<Path> allChildren = stream.toList();
            List<String> subDirs = allChildren.stream()
                    .filter(Files::isDirectory)
                    .map(p -> pathToString(p, detailed))
                    .toList();
            List<String> subFiles = allChildren.stream()
                    .filter(Files::isRegularFile)
                    .map(p -> pathToString(p, detailed))
                    .toList();
            List<String> paths = new ArrayList<>();
            paths.add(((Integer) subDirs.size()).toString());
            paths.addAll(subDirs);
            paths.addAll(subFiles);
            return respond(request, Status.OK, paths.toArray(String[]::new));
        } catch (IOException e) {
            return respond(request, Status.INTERNAL_SERVER_ERROR, new String[]{e.toString()});
        }
    }

    // Before writing this I can't imagine it's the most complex function!!!
    public boolean handleMoveCopy(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (payload.command() != CommandEnum.MOVE && payload.command() != CommandEnum.COPY) return false;
        String[] args = payload.args();
        if (args.length < 3) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        Path src = getVerifiedPath(args[0]);
        Path dest = getVerifiedPath(args[1]);
        boolean recursive = args[2].contains("r");
        boolean isCopy = payload.command() == CommandEnum.COPY;
        if (src == null || !Files.exists(src)) {
            return respond(request, Status.NOT_FOUND, new String[]{"Source not found"});
        }
        if (dest == null) {
            return respond(request, Status.FORBIDDEN, new String[]{"Destination is invalid"});
        }
        if (Files.isDirectory(src) && isCopy && !recursive) {
            return respond(request, Status.FORBIDDEN, new String[]{"Source is directory"});
        }
        try {
            if (Files.exists(dest) && Files.isSameFile(src, dest)) {
                return respond(request, Status.FORBIDDEN, new String[]{"Source equals to destination"});
            }
            if (RemoteUtils.isSubDirectory(src, dest)) {
                return respond(request, Status.FORBIDDEN, new String[]{
                        isCopy ? "Cannot copy a directory into itself" : "Cannot move a directory into itself"
                });
            }
            if (Files.isDirectory(src)) {
                if (Files.exists(dest)) {
                    if (Files.isDirectory(dest)) {
                        Path target = Paths.get(dest.toString(), src.getFileName().toString());
                        if (isCopy) {
                            PathUtils.copyDirectory(src, target, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.move(src, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } else {
                        return respond(request, Status.FORBIDDEN, new String[]{"Source is directory but destination is file"});
                    }
                } else {
                    if (isCopy) {
                        PathUtils.copyDirectory(src, dest);
                    } else {
                        Files.move(src, dest);
                    }
                }
            } else {
                if (Files.exists(dest)) {
                    if (Files.isDirectory(dest)) {
                        if (isCopy) {
                            PathUtils.copyFileToDirectory(src, dest, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            // Cannot use Files.move()
                            PathUtils.copyFileToDirectory(src, dest, StandardCopyOption.REPLACE_EXISTING);
                            Files.delete(src);
                        }
                    } else {
                        if (isCopy) {
                            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } else {
                    if (isCopy) {
                        Files.copy(src, dest);
                    } else {
                        Files.move(src, dest);
                    }
                }
            }
        } catch (IOException e) {
            return respond(request, Status.INTERNAL_SERVER_ERROR, new String[]{e.toString()});
        }
        return respond(request, Status.OK, null);
    }

    public boolean handleRemove(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (payload.command() != CommandEnum.REMOVE) return false;
        String[] args = payload.args();
        if (args.length < 2) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        Path target = getVerifiedPath(args[0]);
        boolean recursive = args[1].contains("r");
        if (target == null || !Files.exists(target)) {
            return respond(request, Status.NOT_FOUND, null);
        }
        try {
            if (Files.isDirectory(target)) {
                if (!recursive) {
                    return respond(request, Status.FORBIDDEN, new String[]{"Target is directory"});
                }
                PathUtils.deleteDirectory(target);
            } else {
                Files.delete(target);
            }
        } catch (IOException e) {
            return respond(request, Status.INTERNAL_SERVER_ERROR, new String[]{e.toString()});
        }
        return respond(request, Status.OK, null);
    }

    public boolean handleMkdirTouch(Request<CommandC2SPayload> request) {
        CommandC2SPayload payload = request.payload();
        if (payload.command() != CommandEnum.MKDIR && payload.command() != CommandEnum.TOUCH) return false;
        String[] args = payload.args();
        if (args.length < 1) {
            return respond(request, Status.BAD_REQUEST, null);
        }
        Path target = getVerifiedPath(args[0]);
        if (target == null) {
            return respond(request, Status.NOT_FOUND, null);
        }
        try {
            if (payload.command() == CommandEnum.MKDIR) {
                if (Files.exists(target)) {
                    return respond(request, Status.FORBIDDEN, null);
                }
                Files.createDirectories(target);
            } else {
                if (!Files.exists(target)) {
                    Files.createFile(target);
                }
                Files.setLastModifiedTime(target, FileTime.from(Instant.now()));
            }
        } catch (IOException e) {
            return respond(request, Status.INTERNAL_SERVER_ERROR, new String[]{e.toString()});
        }
        return respond(request, Status.OK, null);
    }

    // Helpers

    // Must be subdirectory (may not exist)
    private @Nullable Path getVerifiedPath(String p) {
        if (!p.startsWith("/")) {
            return null;
        }
        Path path = Paths.get(p.substring(1));
        if (!RemoteUtils.isSubDirectory(Paths.get(""), path) && !p.equals("/")) {
            return null;
        }
        return path;
    }

    private String pathToString(Path p, boolean detailed) {
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (!detailed) return p.getFileName().toString();
        String latestModified, fileSize;
        try {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(), ZoneId.systemDefault());
            latestModified = dateTime.format(FORMATTER);
        } catch (IOException e) {
            latestModified = "Error";
        }
        if (Files.isDirectory(p)) {
            fileSize = "(DIR)";
        } else {
            try {
                fileSize = RemoteUtils.bytesToString(Files.size(p));
            } catch (IOException e) {
                fileSize = "Error";
            }
        }
        return String.format("%s  %10s  %s", latestModified, fileSize, p.getFileName());
    }
}
