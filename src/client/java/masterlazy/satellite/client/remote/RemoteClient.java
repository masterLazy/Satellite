package masterlazy.satellite.client.remote;

import masterlazy.satellite.client.remote.command.RemoteCommand;
import masterlazy.satellite.remote.payload.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.apache.sshd.server.session.ServerSession;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;

public class RemoteClient {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Handler handler = new Handler(this);
    private final ConsoleSsh consoleSsh = new ConsoleSsh(this);

    private final HashMap<Integer, Object> received = new HashMap<>();
    private final HashSet<Integer> deposedIds = new HashSet<>();
    private final HashMap<ServerSession, String> tokens = new HashMap<>();

    public final String VERSION = "v1";
    private boolean remoteAvailable = false;

    public synchronized void setRemoteAvailable(boolean available) {
        remoteAvailable = available;
    }

    public synchronized boolean isRemoteAvailable() {
        return remoteAvailable;
    }

    public synchronized void putReceived(int requestId, Object payload) {
        received.put(requestId, payload);
    }

    public synchronized boolean isDeposed(int requestId) {
        if (deposedIds.contains(requestId)) {
            deposedIds.remove(requestId);
            return true;
        }
        return false;
    }

    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->{
            RemoteCommand.register(dispatcher, this, consoleSsh);
        });
        // Payloads
        ClientPlayNetworking.registerGlobalReceiver(HelloS2CPayload.ID, handler::handleHelloS2C);
        ClientPlayNetworking.registerGlobalReceiver(AuthorizeS2CPayload.ID, handler::handleAuthorizeS2C);
        ClientPlayNetworking.registerGlobalReceiver(ConsoleCmdS2CPayload.ID, handler::handleConsoleCmdS2C);
        ClientPlayNetworking.registerGlobalReceiver(ConsoleFeedS2CPayload.ID, handler::handleConsoleFeedS2C);
    }

    // TODO: 找一种更安全的方法。。。
    public boolean getTokenForSession(String password, ServerSession session) {
        if (!remoteAvailable) return false;
        int requestId = RANDOM.nextInt();
        ClientPlayNetworking.send(new AuthorizeC2SPayload(requestId, password));
        Instant timeout = Instant.now().plus(Duration.ofSeconds(30));
        while (timeout.isAfter(Instant.now())) {
            try {
                if (received.containsKey(requestId)) {
                    AuthorizeS2CPayload payload = (AuthorizeS2CPayload) received.remove(requestId);
                    if (payload.token().isEmpty()) return false;
                    tokens.put(session, payload.token());
                    return true;
                }
                Thread.sleep(100);
            } catch (Exception ignored) {
                break;
            }
        }
        deposedIds.add(requestId);
        return false;
    }
}
