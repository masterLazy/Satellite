package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.client.remote.RateCounter;
import masterlazy.satellite.client.remote.RemoteClient;
import masterlazy.satellite.client.remote.UnauthorizedException;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.FilePayloadType;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import masterlazy.satellite.remote.payload.FileC2SPayload;
import masterlazy.satellite.remote.payload.FileS2CPayload;
import masterlazy.satellite.remote.pipeline.FileHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static masterlazy.satellite.remote.RemoteUtils.bytesToString;

public class FileCLI {
    private final SatelliteCLI cli;
    private final ShellContext ctx;

    public FileCLI(SatelliteCLI cli, ShellContext ctx) {
        this.cli = cli;
        this.ctx = ctx;
    }

    public void ls(boolean detailed) throws ExecutionException, InterruptedException {
        ListResponse response = list(cli.getWorkingDir(), detailed);
        if (response == null) {
            ctx.println("Path not found");
            if (!cli.workingDir.isEmpty()) {
                cli.workingDir.remove(cli.workingDir.size()-1);
            }
            return;
        }
        int x = 0;
        for (int i = 0; i < response.paths.length; i++) {
            if (i < response.dirCount) {
                ctx.write("\033[34m\033[1m"+response.paths[i]+"\033[0m");
            } else {
                ctx.write(response.paths[i]);
            }
            if (detailed) {
                ctx.write("\r\n");
                continue;
            }
            x += response.paths[i].length();
            if (x >= 80) {
                ctx.write("\r\n");
                x = 0;
            } else {
                for (int j = x; j < x-(x%16)+16; j++) {
                    ctx.write(' ');
                }
                x = x-(x%16)+16;
                if (x >= 80) {
                    ctx.write("\r\n");
                    x = 0;
                }
            }
        }
        if (!detailed) {
            ctx.setSuggestions(response.paths());
        }
        ctx.write("\r\n");
        ctx.flush();
    }

    public void cd(String subdir) throws ExecutionException, InterruptedException {
        String sd = resolve(subdir);
        if (sd == null) return;
        ListResponse response = list(sd, false);
        if (response == null) {
            ctx.println("Directory not found");
        } else {
            cli.workingDir.clear();
            cli.workingDir.addAll(List.of(sd.substring(1).split("/")));
            ctx.setSuggestions(response.paths());
        }
    }

    public void mv_cp(String src, String dest, CommandEnum command, boolean recursive) throws ExecutionException, InterruptedException  {
        String action;
        if (command == CommandEnum.COPY) {
            action = "copy";
        } else if (command == CommandEnum.MOVE) {
            action = "move";
        } else return;
        String s = resolve(src);
        if (s == null) return;
        String d = resolve(dest);
        if (d == null) return;
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(ctx, command, new String[]{s, d, recursive?"r":""});
        if (response == null) return;
        if (response.status() == Status.UNAUTHORIZED) {
            ctx.renewToken();
            response = SatelliteClient.remoteClient.sendAndWait(ctx, command, new String[]{s, d, recursive?"r":""});
            if (response == null) return;
            if (response.status() == Status.UNAUTHORIZED) throw new UnauthorizedException();
        }
        if (response.status() != Status.OK) {
            ctx.reportFailure(action, response);
        }
    }

    public void rm(String target, boolean recursive) throws ExecutionException, InterruptedException {
        String t = resolve(target);
        if (t == null) return;
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(ctx, CommandEnum.REMOVE, new String[]{t, recursive?"r":""});
        if (response == null) return;
        if (response.status() == Status.UNAUTHORIZED) {
            ctx.renewToken();
            response = SatelliteClient.remoteClient.sendAndWait(ctx, CommandEnum.REMOVE, new String[]{t, recursive?"r":""});
            if (response == null) return;
            if (response.status() == Status.UNAUTHORIZED) throw new UnauthorizedException();
        }
        if (response.status() != Status.OK) {
            ctx.reportFailure("remove", response);
        }
    }

    public void mkdir_touch(String target, CommandEnum command) throws ExecutionException, InterruptedException {
        String action;
        if (command == CommandEnum.MKDIR) {
            action = "create directory";
        } else if (command == CommandEnum.TOUCH) {
            action = "touch";
        } else return;
        String t = resolve(target);
        if (t == null) return;
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(ctx, command, new String[]{t});
        if (response == null) return;
        if (response.status() == Status.UNAUTHORIZED) {
            ctx.renewToken();
            response = SatelliteClient.remoteClient.sendAndWait(ctx, command, new String[]{t});
            if (response == null) return;
            if (response.status() == Status.UNAUTHORIZED) throw new UnauthorizedException();
        }
        if (response.status() != Status.OK) {
            ctx.reportFailure(action, response);
        }
    }

    public void get(String target, String savePathStr, boolean override) throws ExecutionException, InterruptedException, IOException {
        String t = resolve(target);
        if (t == null) return;
        String[] ts = t.split("/");
        String filename = ts[ts.length - 1];
        Path savePath = Paths.get(savePathStr);
        Path saveFile, tmpFile;
        if (Files.exists(savePath)) {
            if (Files.isDirectory(savePath)) {
                saveFile = Paths.get(savePathStr, filename);
            } else {
                saveFile = savePath;
            }
        } else {
            Files.createDirectories(savePath);
            saveFile = Paths.get(savePathStr, filename);
        }
        tmpFile = Paths.get(saveFile.getParent().toString(), saveFile.getFileName().toString()+".tmp");
        if (Files.exists(saveFile) && !override) {
            ctx.println("Cannot save to '"+saveFile+"': already exists");
            return;
        }
        if (Files.exists(tmpFile)) {
            ctx.println("Cannot save to '"+tmpFile+"': already exists. Another instance may be downloading; if not, please remove it first.");
            return;
        }
        try {
            Files.createFile(tmpFile);
            // Send command
            CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(ctx, CommandEnum.GET, new String[]{t});
            if (response == null) return;
            if (response.status() == Status.UNAUTHORIZED) {
                ctx.renewToken();
                response = SatelliteClient.remoteClient.sendAndWait(ctx, CommandEnum.GET, new String[]{t});
                if (response == null) return;
                if (response.status() == Status.UNAUTHORIZED) throw new UnauthorizedException();
            }
            if (response.status() != Status.OK) {
                ctx.reportFailure("get", response);
                return;
            }
            UUID sessionId;
            long fileSize;
            try {
                sessionId = UUID.fromString(response.results()[0]);
                fileSize = Long.parseLong(response.results()[1]);
            } catch (Exception e) {
                ctx.println("Server sent invalid response: "+e);
                return;
            }
            // Start session
            int part = 1, receivedCount;
            ByteBuffer[] buffers = new ByteBuffer[FileHandler.BATCH_SIZE];
            BlockingQueue<FileS2CPayload> queue = SatelliteClient.remoteClient.getFileQueueFor(sessionId);
            boolean eof = false;
            RateCounter rateCounter = new RateCounter();
            try (FileChannel fileChannel = FileChannel.open(tmpFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                while (!Thread.currentThread().isInterrupted() && !eof) { // TODO: 添加重试逻辑
                    if (pressedCtrlC()) throw new RuntimeException("Keyboard interruption");
                    // Fetch
                    ClientPlayNetworking.send(new FileC2SPayload(ctx.token(), sessionId, FilePayloadType.FETCH, part, new byte[0]));
                    ctx.print(String.format("\rDownloaded %10s of %10s, \033[36m%10s/s\033[0m",
                            bytesToString(rateCounter.getTotal()),
                            bytesToString(fileSize),
                            bytesToString(rateCounter.getPerSecond())));
                    // Receive
                    receivedCount = 0;
                    for (int i = 0; i < FileHandler.BATCH_SIZE && !Thread.currentThread().isInterrupted(); i++) {
                        if (pressedCtrlC()) throw new RuntimeException("Keyboard interruption");
                        FileS2CPayload received = queue.poll(RemoteClient.FILE_POLL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                        if (received != null) {
                            if (received.payloadType() == FilePayloadType.INTERRUPT) {
                                throw new RuntimeException("Server interrupted session");
                            }
                            int rPart = received.arg();
                            if (rPart < 0) {
                                eof = true;
                                rPart = -rPart;
                            }
                            if (rPart - part >= FileHandler.BATCH_SIZE) {
                                throw new RuntimeException("Server sent invalid part number");
                            }
                            buffers[rPart - part] = ByteBuffer.wrap(received.data());
                            rateCounter.submit(received.data().length);
                            receivedCount++;
                            if (eof) break;
                        } else throw new RuntimeException("Timeout");
                    }
                    if (!eof && receivedCount < FileHandler.BATCH_SIZE) {
                        throw new RuntimeException("Timeout");
                    }
                    part += FileHandler.BATCH_SIZE;
                    // Write to file
                    long totalToWrite = 0, written = 0;
                    for (ByteBuffer b : buffers) totalToWrite += b.remaining();
                    while (written < totalToWrite) {
                        long n = fileChannel.write(buffers, 0, receivedCount);
                        if (n <= 0) throw new RuntimeException("Failed to write to '"+tmpFile+"'");
                        written += n;
                    }
                }
            }
            Files.move(tmpFile, saveFile, StandardCopyOption.REPLACE_EXISTING);
            ctx.println("\r\nDownloaded '" + t + "' to '" + saveFile + "'");
        } catch (RuntimeException e) {
            ctx.println("\r\n\033[31mFailed to download: "+e.getMessage()+"\033[0m");
        } finally {
            if (Files.exists(tmpFile)) {
                Files.delete(tmpFile);
            }
        }
    }

    private @Nullable String resolve(String path) {
        String[] given = path.split("/");
        List<String> goal = new ArrayList<>();
        if (!path.startsWith("/") && !cli.getWorkingDir().equals("/")) goal.addAll(cli.workingDir);
        // Resolve path
        for (String s : given) {
            if (s.equals(".")) continue;
            if (s.equals("..")) {
                if (goal.isEmpty()) {
                    ctx.println("Failed to resolve path '"+path+"'");
                    return null;
                }
                goal.remove(goal.size()-1);
                continue;
            }
            if (s.isEmpty()) {
                ctx.println("Failed to resolve path '"+path+"'");
                return null;
            }
            goal.add(s);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : goal) sb.append('/').append(s);
        if (sb.isEmpty()) sb.append('/');
        return sb.toString();
    }

    private record ListResponse(int dirCount, String[] paths) {}

    private @Nullable FileCLI.ListResponse list(String dir, boolean detailed) throws ExecutionException, InterruptedException {
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(ctx, CommandEnum.LIST, new String[]{dir, detailed ? "l" : ""});
        if (response == null) return null;
        if (response.status() == Status.UNAUTHORIZED) {
            ctx.renewToken();
            response = SatelliteClient.remoteClient.sendAndWait(ctx, CommandEnum.LIST, new String[]{dir, detailed ? "l" : ""});
            if (response == null) return null;
            if (response.status() == Status.UNAUTHORIZED) throw new UnauthorizedException();
        }
        if (response.status() == Status.NOT_FOUND) {
            return null;
        } else if (response.status() != Status.OK) {
            ctx.reportFailure("list", response);
            return null;
        } else if (response.results().length < 1) {
            ctx.println("\033[31mFailed to list: invalid response from server\033[0m");
            return null;
        }
        try {
            String[] results = response.results();
            int dirCount = Integer.parseInt(results[0]);
            if (dirCount > results.length - 1) {
                ctx.println("\033[31mFailed to list: invalid response from server\033[0m");
                return null;
            }
            return new ListResponse(dirCount, Arrays.copyOfRange(results, 1, results.length));
        } catch (NumberFormatException e) {
            ctx.println("\033[31mFailed to list: invalid response from server\033[0m");
            return null;
        }
    }

    private boolean pressedCtrlC() throws IOException {
        int c;
        while (ctx.getReader().ready()) {
            c = ctx.getReader().read();
            if (c == '\003') { // Ctrl+C
                ctx.print("^C");
                return true;
            }
        }
        return false;
    }
}
