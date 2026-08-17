package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.client.remote.UnauthorizedException;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandS2CPayload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.LIST, new String[]{cli.getWorkingDir()});
        if (response == null) return;
        if (response.status() == Status.UNAUTHORIZED) {
            cli.token = cli.tokenSupplier.get();
            if (cli.token == null) throw new UnauthorizedException();
            response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.LIST, new String[]{cli.getWorkingDir()});
            if (response == null) return;
        }
        if (response.status() == Status.NOT_FOUND) {
            System.out.println("Path not found");
            if (!cli.workingDir.isEmpty()) {
                cli.workingDir.remove(cli.workingDir.getLast());
            }
            return;
        } else if (response.status() != Status.OK) {
            System.out.println("\033[31mFailed to list: "+response.status().name()+"\033[0m");
            return;
        } else if (response.results().length < 1) {
            System.out.println("\033[31mFailed to list: invalid response from server\033[0m");
            return;
        }
        try {
            int dirCount = Integer.parseInt(response.results()[0]);
            if (dirCount > response.results().length - 1) {
                System.out.println("\033[31mFailed to list: invalid response from server\033[0m");
                return;
            }
            int x = 0;
            for (int i = 1; i < response.results().length; i++) {
                if (i <= dirCount) {
                    out.write(("\033[34m\033[1m"+response.results()[i]+"\033[0m").getBytes(StandardCharsets.UTF_8));
                } else {
                    out.write(response.results()[i].getBytes(StandardCharsets.UTF_8));
                }
                x += response.results()[i].length();
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
        } catch (NumberFormatException e) {
            System.out.println("\033[31mFailed to list: invalid response from server\033[0m");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
