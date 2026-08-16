package masterlazy.satellite.client.remote.cli;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

public class SatelliteShellFactory implements ShellFactory {
    @Override
    public Command createShell(ChannelSession channel) {
        return new SatelliteShell();
    }
}
