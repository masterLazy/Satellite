package masterlazy.satellite.remote.pipeline;

import masterlazy.satellite.remote.model.Request;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface PayloadHandler<PayloadT extends CustomPacketPayload> {
    boolean handle(Request<PayloadT> request);
}