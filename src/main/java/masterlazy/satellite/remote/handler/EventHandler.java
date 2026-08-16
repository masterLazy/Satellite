package masterlazy.satellite.remote.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.payload.HelloS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class EventHandler {
    private final RemoteService service;

    public EventHandler(RemoteService remoteService) {
        service = remoteService;
    }

    public void register() {
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) ->
                onPlayerJoin(listener.getPlayer()));
    }

    private void onPlayerJoin(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, HelloS2CPayload.ID.id())) {
            HelloS2CPayload payload = new HelloS2CPayload(service.VERSION);
            ServerPlayNetworking.send(player, payload);
            Satellite.B_LOGGER.debug("%s << HelloS2CPayload:\n%s", player.getName().getString(), Satellite.GSON.toJson(payload));
        }
    }
}
