package masterlazy.satellite.client.remote;

import masterlazy.satellite.Satellite;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.IOException;
import java.nio.file.Paths;

public class ConsoleSsh {
    private final SshServer sshd;

    public ConsoleSsh(RemoteClient client) {
        sshd = SshServer.setUpDefaultServer();
        sshd.setShellFactory(new ConsoleShellFactory());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(Satellite.BASE_DIR + "hostkey.ser")));
        sshd.setPasswordAuthenticator((username, password, session) -> {
            return client.getTokenForSession(password, session);
        });
    }

    // TODO: 启动、停止时添加状态检查

    public int start() {
        try {
            sshd.start();
            Satellite.LOGGER.info("[Satellite Client] Remote console SSH server is running on port: {}", sshd.getPort());
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite Client] Failed to start remote console SSH server", e);
            return -1;
        }
        return sshd.getPort();
    }

    public boolean stop() {
        try {
            sshd.stop();
            Satellite.LOGGER.info("[Satellite Client] Remote console SSH server stopped");
            return true;
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite Client] Failed to stop remote console SSH server", e);
            return false;
        }
    }
}
