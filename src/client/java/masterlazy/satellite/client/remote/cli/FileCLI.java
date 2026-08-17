package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.client.remote.UnauthorizedException;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FileCLI {
    private final SatelliteCLI cli;
    private final BufferedReader reader;
    private final OutputStream out;

    public FileCLI(SatelliteCLI cli) {
        this.cli = cli;
        reader = cli.reader;
        out = cli.out;
    }

    void ls() throws ExecutionException, InterruptedException {
        ListResponse response = list(cli.getWorkingDir());
        if (response == null) {
            System.out.println("Path not found");
            if (!cli.workingDir.isEmpty()) {
                cli.workingDir.remove(cli.workingDir.getLast());
            }
            return;
        }
        try {
            int x = 0;
            for (int i = 0; i < response.paths.length; i++) {
                if (i <= response.dirCount) {
                    out.write(("\033[34m\033[1m"+response.paths[i]+"\033[0m").getBytes(StandardCharsets.UTF_8));
                } else {
                    out.write(response.paths[i].getBytes(StandardCharsets.UTF_8));
                }
                x += response.paths[i].length();
                if (x >= 80) {
                    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    x = 0;
                } else {
                    for (int j = x; j < x-(x%16)+16; j++) {
                        out.write(' ');
                    }
                    x = x-(x%16)+16;
                    if (x >= 80) {
                        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                        x = 0;
                    }
                }
            }
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void cd(String subdir) throws ExecutionException, InterruptedException {
        String[] given = subdir.split("/");
        List<String> goal = new ArrayList<>();
        if (!subdir.startsWith("/")) goal.addAll(cli.workingDir);
        // Resolve path
        for (String s : given) {
            if (s.equals(".")) continue;
            if (s.equals("..")) {
                if (goal.isEmpty()) {
                    System.out.println("Directory not found");
                    return;
                }
                goal.removeLast();
                continue;
            }
            if (s.isEmpty()) {
                System.out.println("Directory not found");
                return;
            }
            goal.add(s);
        }
        // Make request
        StringBuilder sb = new StringBuilder();
        for (String s : goal) sb.append('/').append(s);
        if (sb.isEmpty()) sb.append('/');
        ListResponse response = list(sb.toString());
        if (response == null) {
          System.out.println("Directory not found");
        } else {
            cli.workingDir.clear();
            cli.workingDir.addAll(goal);
        }
    }

    private record ListResponse(int dirCount, String[] paths) {}

    @Nullable
    private FileCLI.ListResponse list(String dir) throws ExecutionException, InterruptedException {
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.LIST, new String[]{dir});
        if (response == null) return null;
        if (response.status() == Status.UNAUTHORIZED) {
            cli.token = cli.tokenSupplier.get();
            if (cli.token == null) throw new UnauthorizedException();
            response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.LIST, new String[]{dir});
            if (response == null) return null;
        }
        if (response.status() == Status.NOT_FOUND) {
            return null;
        } else if (response.status() != Status.OK) {
            System.out.println("\033[31mFailed to list: "+response.status().name()+"\033[0m");
            return null;
        } else if (response.results().length < 1) {
            System.out.println("\033[31mFailed to list: invalid response from server\033[0m");
            return null;
        }
        try {
            String[] results = response.results();
            int dirCount = Integer.parseInt(results[0]);
            if (dirCount > results.length - 1) {
                System.out.println("\033[31mFailed to list: invalid response from server\033[0m");
                return null;
            }
            return new ListResponse(dirCount, Arrays.copyOfRange(results, 1, results.length));
        } catch (NumberFormatException e) {
            System.out.println("\033[31mFailed to list: invalid response from server\033[0m");
            return null;
        }
    }
}
