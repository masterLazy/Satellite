package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.Satellite;
import org.apache.sshd.server.auth.keyboard.InteractiveChallenge;
import org.apache.sshd.server.auth.keyboard.KeyboardInteractiveAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class SshServer {
    private org.apache.sshd.server.SshServer sshd;

    private static final int PREFERRED_PORT = 2121;

    private boolean running;

    public SshServer() {}

    private void setupSshd() {
        sshd = org.apache.sshd.server.SshServer.setUpDefaultServer();
        sshd.setShellFactory(new SatelliteShellFactory());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(Satellite.BASE_DIR, "hostkey.ser")));
        sshd.setHost("127.0.0.1"); // localhost
        // Config none verification
        sshd.setPasswordAuthenticator((u, p, ss) -> true);
        sshd.setPublickeyAuthenticator((s, u, ss) -> false);
        sshd.setKeyboardInteractiveAuthenticator(new KeyboardInteractiveAuthenticator() {
            @Override
            public InteractiveChallenge generateChallenge(ServerSession session, String username, String lang, String subMethods) {
                return new InteractiveChallenge();
            }
            @Override
            public boolean authenticate(ServerSession session, String username, List<String> responses) {
                return true;
            }
        });
    }

    public boolean isRunning() {
        return running;
    }

    public int start() {
        setupSshd();
        try {
            sshd.setPort(PREFERRED_PORT);
            sshd.start();
        } catch (IOException e) {
            sshd.setPort(0);
            try {
                sshd.start();
            } catch (IOException e1) {
                Satellite.LOGGER.error("[Satellite Client] Failed to start SSH server", e1);
                return -1;
            }
        }
        running = true;
        Satellite.LOGGER.info("[Satellite Client] Remote console SSH server is running on port: {}", sshd.getPort());
        return sshd.getPort();
    }

    public boolean close() {
        try {
            sshd.close(true);
            running = false;
            Satellite.LOGGER.info("[Satellite Client] Remote console SSH server closed");
            return true;
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite Client] Failed to close remote console SSH server", e);
            return false;
        }
    }
}
