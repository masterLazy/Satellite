package masterlazy.satellite.client.remote;

import masterlazy.satellite.remote.payload.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context;

public class Handler {
    private final RemoteClient client;

    public Handler(RemoteClient client) {
        this.client = client;
    }

    public void handleAuthorizeS2C(AuthorizeS2CPayload payload, Context context) {
        int id = payload.requestId();
        if (client.isDeposed(id)) return;
        client.putReceived(id, payload);
    }

    public void handleHelloS2C(HelloS2CPayload payload, Context context) {
        if (ClientPlayNetworking.canSend(HelloC2SPayload.ID.id()) && payload.version().equals(client.VERSION)) {
            ClientPlayNetworking.send(new HelloC2SPayload(true));
            client.setRemoteAvailable(true);
        }
    }

    public void handleConsoleCmdS2C(ConsoleCmdS2CPayload payload, Context context) {

    }

    public void handleConsoleFeedS2C(ConsoleFeedS2CPayload payload, Context context) {

    }
}
