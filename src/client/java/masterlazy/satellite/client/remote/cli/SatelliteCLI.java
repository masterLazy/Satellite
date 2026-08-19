package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.remote.model.CommandEnum;
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
    @Command(name = "pwd", description = "Display the current directory path.")
    public void pwd() {
        ctx.println(getWorkingDir());
    }

    @SuppressWarnings("unused")
    @Command(name = "console", description = "Connect to Minecraft server console.")
    public void console() throws ExecutionException, InterruptedException {
        consoleCLI.run();
    }

    @SuppressWarnings("unused")
    @Command(name = "ls", description = "List files and directories.")
    public void ls(
            @CommandLine.Option(names = {"-l", "--long"}, description = "list detailed info") boolean detailed
    ) throws ExecutionException, InterruptedException {
        fileCLI.ls(detailed);
    }

    @SuppressWarnings("unused")
    @Command(name = "cd", description = "Change the current directory.")
    public void cd(
            @CommandLine.Parameters(paramLabel = "<subdirectory>") String subdir
    ) throws ExecutionException, InterruptedException {
        fileCLI.cd(subdir);
    }

    @SuppressWarnings("unused")
    @Command(name = "mv", description = "Move files or renames them. \033[33mALWAYS REPLACE EXISTING FILES\033[0m.")
    public void mv(
            @CommandLine.Parameters(paramLabel = "<source>") String src,
            @CommandLine.Parameters(paramLabel = "<destination>") String dest
    ) throws ExecutionException, InterruptedException {
        fileCLI.mv_cp(src, dest, CommandEnum.MOVE, false);
    }

    @SuppressWarnings("unused")
    @Command(name = "cp", description = "Copy files from source to destination. \033[33mALWAYS REPLACE EXISTING FILES\033[0m.")
    public void cp(
            @CommandLine.Parameters(paramLabel = "<source>") String src,
            @CommandLine.Parameters(paramLabel = "<destination>") String dest,
            @CommandLine.Option(names = {"-r", "--recursive"}, description = "recursively copy") boolean recursive
    ) throws ExecutionException, InterruptedException {
        fileCLI.mv_cp(src, dest, CommandEnum.COPY, recursive);
    }

    @SuppressWarnings("unused")
    @Command(name = "rm", description = "Delete a file.")
    public void rm(
            @CommandLine.Parameters(paramLabel = "<target>") String target,
            @CommandLine.Option(names = {"-r", "--recursive"}, description = "recursively copy") boolean recursive
    ) throws ExecutionException, InterruptedException {
        fileCLI.rm(target, recursive);
    }

    @SuppressWarnings("unused")
    @Command(name = "mkdir", description = "Create a new directory.")
    public void mkdir(
            @CommandLine.Parameters(paramLabel = "<target>") String target
    ) throws ExecutionException, InterruptedException {
        fileCLI.mkdir_touch(target, CommandEnum.MKDIR);
    }

    @SuppressWarnings("unused")
    @Command(name = "touch", description = "Create an empty file or updates the last accessed date.")
    public void touch(
            @CommandLine.Parameters(paramLabel = "<target>") String target
    ) throws ExecutionException, InterruptedException {
        fileCLI.mkdir_touch(target, CommandEnum.TOUCH);
    }
}
