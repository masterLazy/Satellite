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

public class SatelliteShell implements Command, Runnable {
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Thread thread;

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

            SatelliteCLI cli = new SatelliteCLI(this::authorize, reader, out);
            CommandLine cmd = new CommandLine(cli);
            cmd.setOut(new PrintWriter(out, true));
            cmd.setErr(new PrintWriter(err, true));

            writeAndFlush("\033[2J\033[H"); // Clear screen
            String welcome = """
                    Welcome to \r
                      \033[36m███████╗ █████╗ ████████╗███████╗██╗     ██╗     ██╗████████╗███████╗\033[0m     ██████╗██╗     ██╗\r
                      \033[36m██╔════╝██╔══██╗╚══██╔══╝██╔════╝██║     ██║     ██║╚══██╔══╝██╔════╝\033[0m    ██╔════╝██║     ██║\r
                      \033[36m███████╗███████║   ██║   █████╗  ██║     ██║     ██║   ██║   █████╗  \033[0m    ██║     ██║     ██║\r
                      \033[36m╚════██║██╔══██║   ██║   ██╔══╝  ██║     ██║     ██║   ██║   ██╔══╝  \033[0m    ██║     ██║     ██║\r
                      \033[36m███████║██║  ██║   ██║   ███████╗███████╗███████╗██║   ██║   ███████╗\033[0m    ╚██████╗███████╗██║\r
                      \033[36m╚══════╝╚═╝  ╚═╝   ╚═╝   ╚══════╝╚══════╝╚══════╝╚═╝   ╚═╝   ╚══════╝\033[0m     ╚═════╝╚══════╝╚═╝\r
                    """;
            writeAndFlush(welcome);
            writeAndFlush("Type 'exit' to quit; type '-h' for usage.\r\n\r\n");

            while (!Thread.currentThread().isInterrupted()) {
                writeAndFlush(cli.getPrompt());
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

    @Nullable
    private String authorize() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                writeAndFlush("\r\n[Authorize] Password for '" + SatelliteClient.getUserName() + "': ");
                String password = readLineWithEcho(true);
                if (password == null) {
                    writeAndFlush("\r\n Authorization failed");
                    break;
                }
                if (password.trim().isEmpty()) continue;
                try {
                    CommandS2CPayload response = SatelliteClient.remoteClient.sendCommand("", CommandEnum.AUTHORIZE, new String[]{password}).get();
                    if (response.status() == Status.OK) {
                        if (response.results().length < 1) {
                            writeAndFlush("Authorization failed: Server didn't respond a token.\r\n");
                            continue;
                        }
                        writeAndFlush("Success.\r\n");
                        return response.results()[0];
                    } else if (response.status() == Status.UNAUTHORIZED) {
                        writeAndFlush("Wrong password.\r\n");
                    } else if (response.status() == Status.TOO_MANY_REQUEST) {
                        writeAndFlush("\033[31mAuthorization rate limit exceeded. Try later.\033[0m\r\n");
                    } else {
                        writeAndFlush("Authorization failed: " + response.status().name() + "\r\n");
                    }
                } catch (Exception e) {
                    err.write(("\033[31mError: " + e.getMessage() + "\033[0m\r\n").getBytes(StandardCharsets.UTF_8));
                    err.flush();
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void writeAndFlush(String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void write(String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.UTF_8));
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
                    writeAndFlush("\b");
                    cursorAt--;
                } else if (c == 'C' && cursorAt < sb.length()) { // Right
                    writeAndFlush("\033[C");
                    cursorAt++;
                } else if (c == 'A' && historyCursorAt > 0) { // Up
                    historyCursorAt--;
                    for (; cursorAt > 0; cursorAt--) write("\b \b");
                    write(inputHistory.get(historyCursorAt));
                    out.flush();
                    sb.setLength(0);
                    sb.append(inputHistory.get(historyCursorAt));
                    cursorAt = sb.length();
                } else if (c == 'B' && historyCursorAt + 1 < inputHistory.size()) { // Down
                    historyCursorAt++;
                    for (; cursorAt > 0; cursorAt--) write("\b \b");
                    write(inputHistory.get(historyCursorAt));
                    out.flush();
                    sb.setLength(0);
                    sb.append(inputHistory.get(historyCursorAt));
                    cursorAt = sb.length();
                }
                enteringSeq = false;
            } else if (c == '\r' || c == '\n') {
                writeAndFlush("\r\n");
                break;
            } else if (c == '\003') { // Ctrl+C
                sb.setLength(0);
                writeAndFlush("^C\n");
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
                        if (masked) out.write('*');
                        else out.write(sb.charAt(i));
                    }
                    write("\033[K");
                    for (int i = sb.length() - 1; i >= cursorAt; i--) {
                        write("\b");
                    }
                    out.flush();
                }
            } else {
                sb.insert(cursorAt, (char) c);
                // Refresh
                for (int i = cursorAt; i < sb.length(); i++) {
                    if (masked) out.write('*');
                    else out.write(sb.charAt(i));
                }
                for (int i = sb.length() - 1; i > cursorAt; i--) {
                    out.write('\b');
                }
                out.flush();
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
