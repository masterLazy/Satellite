package masterlazy.satellite.remote.handler;

import masterlazy.satellite.remote.CompressedLoad;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.SubscribeManager;
import masterlazy.satellite.remote.model.ConsoleCmdEnum;
import masterlazy.satellite.remote.model.RequestResult;
import masterlazy.satellite.remote.payload.ConsoleCmdC2SPayload;
import masterlazy.satellite.remote.payload.ConsoleCmdS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;

import java.nio.charset.StandardCharsets;

public class ConsoleCmdHandler {
    private final RemoteService service;
    private final SubscribeManager subscribeManager;

    public ConsoleCmdHandler(RemoteService service, SubscribeManager subscribeManager) {
        this.service = service;
        this.subscribeManager = subscribeManager;
    }

    public void handleConsoleCmdC2S(ConsoleCmdC2SPayload payload, Context context) {
        RequestResult r = service.verifyRequest(payload.token(), context);
        if (r != RequestResult.OK) {
            ServerPlayNetworking.send(context.player(), new ConsoleCmdS2CPayload(r.name(), null));
            return;
        }
        ConsoleCmdEnum command = ConsoleCmdEnum.from(payload.command());
        if (command == null) {
            ServerPlayNetworking.send(context.player(), new ConsoleCmdS2CPayload(RequestResult.BAD_REQUEST.name(), null));
            return;
        }
        if (!subscribeManager.isRemoteConsoleAvailable()) {
            ServerPlayNetworking.send(context.player(), new ConsoleCmdS2CPayload(RequestResult.INTERNAL_SERVER_ERROR.name(), null));
        }
        switch (command) {
            case SUBSCRIBE -> {
                subscribeManager.subscribe(payload.token());
                ServerPlayNetworking.send(context.player(), new ConsoleCmdS2CPayload(RequestResult.OK.name(), null));
            }
            case UNSUBSCRIBE -> {
                if (subscribeManager.unsubscribe(payload.token())) {
                    ServerPlayNetworking.send(context.player(), new ConsoleCmdS2CPayload(RequestResult.OK.name(), null));
                } else {
                    ServerPlayNetworking.send(context.player(), new ConsoleCmdS2CPayload(RequestResult.CONFLICT.name(), null));
                }
            }
            case FETCH_1000 -> {
                ServerPlayNetworking.send(context.player(), new ConsoleCmdS2CPayload(
                        RequestResult.OK.name(),
                        new CompressedLoad(subscribeManager.getLast1000Lines().getBytes(StandardCharsets.UTF_8))
                ));
            }
        }
    }
}
