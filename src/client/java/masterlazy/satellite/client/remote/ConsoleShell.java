package masterlazy.satellite.client.remote;

import java.io.*;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

public class ConsoleShell implements Command, Runnable {
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Thread thread;

    // 以下 setter 方法没有变化
    @Override
    public void setInputStream(InputStream in) { this.in = in; }
    @Override
    public void setOutputStream(OutputStream out) { this.out = out; }
    @Override
    public void setErrorStream(OutputStream err) { this.err = err; }
    @Override
    public void setExitCallback(ExitCallback callback) { this.callback = callback; }

    @Override
    public void start(ChannelSession session, Environment env) {
        thread = new Thread(this, "ConsoleShell");
        thread.start();
    }

    @Override
    public void destroy(ChannelSession session) {
        if (thread != null) thread.interrupt();
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            out.write("\033[H\033[2J".getBytes());
            out.write("\033[36m*** Welcome to Satellite Remote Console CLI! ***\r\nType 'exit' to quit.\033[0m\r\n".getBytes());
            out.flush();
            String line;
            while ((line = reader.readLine()) != null) {
                if ("exit".equalsIgnoreCase(line.trim())) {
                    break;
                }
                out.write(("You said: " + line + "\r\n").getBytes());
                out.flush();
            }
        } catch (Exception e) {
            // Client disconnected
        } finally {
            callback.onExit(0);
        }
    }
}
