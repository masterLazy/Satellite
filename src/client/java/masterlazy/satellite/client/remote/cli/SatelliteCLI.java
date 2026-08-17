package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Command(name = "satellite", mixinStandardHelpOptions = true, version = "1.0")
public class SatelliteCLI {
    public final List<String> workingDir = new ArrayList<>();
    public String token = "";
    public final Supplier<String> tokenSupplier;
    public BufferedReader reader;
    public OutputStream out;

    private final ConsoleCLI consoleCLI;
    private final FileCLI fileCLI;

    public SatelliteCLI(Supplier<String> tokenSupplier, BufferedReader reader, OutputStream out) {
        this.tokenSupplier = tokenSupplier;
        this.reader = reader;
        this.out = out;
        // Then initialize sub-CLIs
        consoleCLI = new ConsoleCLI(this);
        fileCLI = new FileCLI(this);
    }

    public String getWorkingDir() {
        if (workingDir.isEmpty()) return "/";
        StringBuilder sb = new StringBuilder();
        for (String s : workingDir) {
            sb.append('/').append(s);
        }
        return sb.toString();
    }

    public String getPrompt() {
        return "\033[32m"+SatelliteClient.getUserName()+"@"+SatelliteClient.getServerName()+"\033[0m:\033[36m"+ getWorkingDir()+"\033[0m$ ";
    }

    @Command(name = "clear", description = "Clear the screen.")
    public void clear() {
        System.out.print("\033[2J\033[H\033[3J"); // Including scroll-back buffer!
    }

    @Command(name = "pwd", description = "Print working directory.")
    public void pwd() {
        System.out.println(getWorkingDir());
    }

    @Command(name = "console", description = "Connect to Minecraft server console.")
    public void console() throws ExecutionException, InterruptedException {
        consoleCLI.run();
    }

    @Command(name = "ls", description = "List sub-directories and files in working directory.")
    public void ls() throws ExecutionException, InterruptedException {
        fileCLI.ls();
    }
}
