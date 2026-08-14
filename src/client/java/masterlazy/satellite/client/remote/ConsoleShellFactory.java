package masterlazy.satellite.client.remote;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

public class ConsoleShellFactory implements ShellFactory {
    @Override
    public Command createShell(ChannelSession channel) {
        return new ConsoleShell();
    }
}
