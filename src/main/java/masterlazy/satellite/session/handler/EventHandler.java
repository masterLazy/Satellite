package masterlazy.satellite.session.handler;

import masterlazy.satellite.session.SessionService;
import masterlazy.satellite.session.PlayerSession;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class EventHandler {
    private final SessionService service;

    public EventHandler(SessionService sessionService) {
        service = sessionService;
    }

    public void register() {
        ServerPlayConnectionEvents.INIT.register((listener, server) ->
            onPlayerInit(listener.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) ->
            onServerPlayerDisconnect(listener.getPlayer()));
    }

    public void onPlayerInit(ServerPlayer player) {
        service.registerSession(player);
    }

    public void onServerPlayerDisconnect(ServerPlayer player) {
        PlayerSession session = service.getSession(player);
        if (session == null) return;
        if (session.isFrozen()) session.restorePlayer();
        service.unregisterSession(player);
    }
}
