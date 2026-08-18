package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Command(name = "satellite", mixinStandardHelpOptions = true, version = "1.0")
public class SatelliteCLI {
    public final List<String> workingDir = new ArrayList<>();

    private final ShellContext ctx;
    private final ConsoleCLI consoleCLI;
    private final FileCLI fileCLI;

    public SatelliteCLI(ShellContext ctx) {
        this.ctx = ctx;
        // Then initialize sub-CLIs
        consoleCLI = new ConsoleCLI(this, ctx);
        fileCLI = new FileCLI(this, ctx);
    }

    public String getWorkingDir() {
        StringBuilder sb = new StringBuilder();
        for (String s : workingDir) sb.append('/').append(s);
        if (sb.isEmpty()) sb.append('/');
        return sb.toString();
    }

    public String getPrompt() {
        return "\033[32m"+SatelliteClient.getUserName()+"@"+SatelliteClient.getServerName()+"\033[0m:\033[36m"+ getWorkingDir()+"\033[0m$ ";
    }

    @SuppressWarnings("unused")
    @Command(name = "clear", description = "Clear the screen.")
    public void clear() {
        ctx.print("\033[2J\033[H\033[3J"); // Including scroll-back buffer!
    }

    @SuppressWarnings("unused")
    @Command(name = "pwd", description = "Print working directory.")
    public void pwd() {
        ctx.println(getWorkingDir());
    }

    @SuppressWarnings("unused")
    @Command(name = "console", description = "Connect to Minecraft server console.")
    public void console() throws ExecutionException, InterruptedException {
        consoleCLI.run();
    }

    @SuppressWarnings("unused")
    @Command(name = "ls", description = "List sub-directories and files in working directory.")
    public void ls(
            @CommandLine.Option(names = {"-l", "--long"}, description = "list detailed info") boolean detailed
    ) throws ExecutionException, InterruptedException {
        fileCLI.ls(detailed);
    }

    @SuppressWarnings("unused")
    @Command(name = "cd", description = "Change working directory.")
    public void cd(
            @CommandLine.Parameters(paramLabel = "<sub-dir>", description = "subdirectory") String subdir
    ) throws ExecutionException, InterruptedException {
        fileCLI.cd(subdir);
    }
}
