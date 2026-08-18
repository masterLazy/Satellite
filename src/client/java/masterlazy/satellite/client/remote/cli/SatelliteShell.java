package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SatelliteShell implements Command, Runnable, ShellContext {
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Thread thread;

    private String myToken = "";

    private BufferedReader reader;
    private final List<String> inputHistory = new ArrayList<>();

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(ChannelSession session, Environment env) {
        thread = new Thread(this, "SatelliteShell");
        thread.start();
    }

    @Override
    public void destroy(ChannelSession session) {
        if (thread != null) thread.interrupt();
    }

    @Override
    public void run() {
        try {
            // Redirect output streams
            PrintStream psOut = new PrintStream(out, true, StandardCharsets.UTF_8);
            PrintStream psErr = new PrintStream(err, true, StandardCharsets.UTF_8);
            System.setOut(psOut);
            System.setErr(psErr);

            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            SatelliteCLI cli = new SatelliteCLI(this);
            CommandLine cmd = new CommandLine(cli);
            cmd.setOut(new PrintWriter(out, true));
            cmd.setErr(new PrintWriter(err, true));

            print("\033[2J\033[H"); // Clear screen
            String welcome = """
                    Welcome to \r
                      \033[36m███████╗ █████╗ ████████╗███████╗██╗     ██╗     ██╗████████╗███████╗\033[0m     ██████╗██╗     ██╗\r
                      \033[36m██╔════╝██╔══██╗╚══██╔══╝██╔════╝██║     ██║     ██║╚══██╔══╝██╔════╝\033[0m    ██╔════╝██║     ██║\r
                      \033[36m███████╗███████║   ██║   █████╗  ██║     ██║     ██║   ██║   █████╗  \033[0m    ██║     ██║     ██║\r
                      \033[36m╚════██║██╔══██║   ██║   ██╔══╝  ██║     ██║     ██║   ██║   ██╔══╝  \033[0m    ██║     ██║     ██║\r
                      \033[36m███████║██║  ██║   ██║   ███████╗███████╗███████╗██║   ██║   ███████╗\033[0m    ╚██████╗███████╗██║\r
                      \033[36m╚══════╝╚═╝  ╚═╝   ╚═╝   ╚══════╝╚══════╝╚══════╝╚═╝   ╚═╝   ╚══════╝\033[0m     ╚═════╝╚══════╝╚═╝\r
                    """;
            print(welcome);
            print("Type 'exit' to quit; type '-h' for usage.\r\n\r\n");

            while (!Thread.currentThread().isInterrupted()) {
                print(cli.getPrompt());
                String line = readLineWithEcho(false);
                if (line == null || "exit".equalsIgnoreCase(line.trim())) {
                    break;
                }
                if (line.trim().isEmpty()) continue;
                String[] args = line.split("\\s+");
                try {
                    cmd.execute(args);
                } catch (Exception e) {
                    err.write(("\033[31mError: " + e.getMessage() + "\033[0m\r\n").getBytes(StandardCharsets.UTF_8));
                    err.flush();
                }
            }
        } catch (Exception ignored) {
        } finally {
            callback.onExit(0);
        }
    }

    @Nullable public String token() { return myToken; }

    public BufferedReader getReader() { return reader; }

    public void renewToken() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                print("\r\n[Authorize] Password for '" + SatelliteClient.getUserName() + "': ");
                String password = readLineWithEcho(true);
                if (password == null) {
                    print("\r\n Authorization failed");
                    break;
                }
                if (password.trim().isEmpty()) continue;
                try {
                    CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(this, CommandEnum.AUTHORIZE, new String[]{password});
                    if (response == null) {
                        break;
                    } else if (response.status() == Status.OK) {
                        if (response.results().length < 1) {
                            println("Authorization failed: Server didn't respond a token.");
                            continue;
                        }
                        println("Success.");
                        myToken = response.results()[0];
                        return;
                    } else if (response.status() == Status.UNAUTHORIZED) {
                        println("Wrong password.");
                    } else if (response.status() == Status.TOO_MANY_REQUEST) {
                        println("\033[31mAuthorization rate limit exceeded. Try later.\033[0m");
                    } else {
                        println("Authorization failed: " + response.status().name());
                    }
                } catch (Exception e) {
                    err.write(("\033[31mError: " + e.getMessage() + "\033[0m\r\n").getBytes(StandardCharsets.UTF_8));
                    err.flush();
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        myToken = null;
    }

    public void write(String s) {
        try {
            out.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }

    public void flush() {
        try {
            out.flush();
        } catch (IOException ignored) {}
    }

    @Nullable
    public String readLineWithEcho(boolean masked) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        boolean enteredEsc = false, enteringSeq = false;
        int cursorAt = 0;
        int historyCursorAt = inputHistory.size();
        while ((c = reader.read()) != -1) {
            if (enteredEsc) {
                if (c == '[') enteringSeq = true;
                enteredEsc = false;
            } else if (enteringSeq) {
                if (c == 'D' && cursorAt > 0) { // Left
                    print("\b");
                    cursorAt--;
                } else if (c == 'C' && cursorAt < sb.length()) { // Right
                    print("\033[C");
                    cursorAt++;
                } else if (c == 'A' && historyCursorAt > 0) { // Up
                    historyCursorAt--;
                    for (; cursorAt > 0; cursorAt--) write("\b \b");
                    write(inputHistory.get(historyCursorAt));
                    flush();
                    sb.setLength(0);
                    sb.append(inputHistory.get(historyCursorAt));
                    cursorAt = sb.length();
                } else if (c == 'B' && historyCursorAt + 1 < inputHistory.size()) { // Down
                    historyCursorAt++;
                    for (; cursorAt > 0; cursorAt--) write("\b \b");
                    write(inputHistory.get(historyCursorAt));
                    flush();
                    sb.setLength(0);
                    sb.append(inputHistory.get(historyCursorAt));
                    cursorAt = sb.length();
                }
                enteringSeq = false;
            } else if (c == '\r' || c == '\n') {
                print("\r\n");
                break;
            } else if (c == '\003') { // Ctrl+C
                sb.setLength(0);
                print("^C\n");
                return null; // Exit
            } else if (c == '\033') { // Esc
                enteredEsc = true;
            } else if (c == '\b' || c == 127) { // Backspace
                if (cursorAt > 0) {
                    sb.deleteCharAt(cursorAt - 1);
                    cursorAt--;
                    write("\b \b");
                    // Refresh
                    for (int i = cursorAt; i < sb.length(); i++) {
                        if (masked) write('*');
                        else write(sb.charAt(i));
                    }
                    write("\033[K");
                    for (int i = sb.length() - 1; i >= cursorAt; i--) {
                        write("\b");
                    }
                    flush();
                }
            } else if (c == '\t') {
                continue;
            } else {
                sb.insert(cursorAt, (char) c);
                // Refresh
                for (int i = cursorAt; i < sb.length(); i++) {
                    if (masked) write('*');
                    else write(sb.charAt(i));
                }
                for (int i = sb.length() - 1; i > cursorAt; i--) {
                    write('\b');
                }
                flush();
                cursorAt++;
            }
        }
        if (c == -1 && sb.isEmpty()) {
            return null;
        }
        inputHistory.add(sb.toString());
        return sb.toString();
    }
}
