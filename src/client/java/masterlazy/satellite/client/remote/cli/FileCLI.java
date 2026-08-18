package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.client.remote.UnauthorizedException;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
        }
    }

    public void mvcp(String src, String dest, boolean isCopy, boolean recursive) throws ExecutionException, InterruptedException  {
        CommandEnum command = isCopy ? CommandEnum.COPY : CommandEnum.MOVE;
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
            if (response.results().length < 1) {
                ctx.println("\033[31mFailed to "+(isCopy?"copy":"move")+": "+response.status().name() + "\033[0m");
            } else {
                ctx.println("\033[31mFailed to " + (isCopy ? "copy" : "move") + ": " + response.status().name() + " " + response.results()[0] + "\033[0m");
            }
        }
        ctx.println("Done.");
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
                    ctx.println("Path not found");
                    return null;
                }
                goal.remove(goal.size()-1);
                continue;
            }
            if (s.isEmpty()) {
                ctx.println("Path not found");
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
            ctx.println("\033[31mFailed to list: "+response.status().name()+"\033[0m");
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
}
