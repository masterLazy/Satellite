package masterlazy.satellite.remote.model;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.server.level.ServerPlayer;

public record Request <PayloadT extends CustomPacketPayload> (
    PayloadT payload,
    Context ctx
) {
    public ServerPlayer player() {
        return ctx.player();
    }

    public String sender() {
        return  ctx.player().getName().getString();
    }

    public void respond(CustomPacketPayload payload) {
        ctx.responseSender().sendPacket(payload);
    }
}
