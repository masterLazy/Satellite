package masterlazy.satellite.auth;

import masterlazy.satellite.SessionManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class AuthSessionManager extends SessionManager<AuthSession> {
    public void onInitialize() {
        ServerPlayConnectionEvents.INIT.register((listener, server) -> onPlayerInit(listener.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> onServerPlayerDisconnect(listener.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> onServerStopping());
    }

    private void onPlayerInit(ServerPlayer player) {
        AuthSession session = get(player.getUUID());
        if (session == null) {
            session = new AuthSession(player);
            register(session);
        }
        session.setTempPlayer(player);
    }

    private void onServerPlayerDisconnect(ServerPlayer player) {
        AuthSession session = get(player.getUUID());
        if (session == null) return;
        if (session.isFrozen()) session.restorePlayer();
        if (session.isLoggedIn() || session.rateLimit.tryAcquire()) expire(session);
        session.setTempPlayer(null);
    }

    private void onServerStopping() {
        for (AuthSession session : sessionMap.values()) {
            if (session == null) return;
            if (session.isFrozen()) session.restorePlayer();
        }
    }
}
