package masterlazy.satellite.session;

import masterlazy.satellite.session.model.PlayerSession;
import net.minecraft.server.level.ServerPlayer;

public class SessionHandler {
    private final SessionManager manager;

    public SessionHandler(SessionManager sessionManager) {
        manager = sessionManager;
    }

    public void onServerPlayConnectionInit(ServerPlayer player) {
        manager.registerSession(player);
    }

    public void onServerPlayConnectionDisconnect(ServerPlayer player) {
        PlayerSession session = manager.getSession(player);
        if (session == null) return;
        if (session.isFroze()) session.restorePlayer();
        manager.unregisterSession(player);
    }
}
