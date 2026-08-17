package masterlazy.satellite.remote.pipeline;

import masterlazy.satellite.remote.model.Request;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Pipeline <PayloadT extends CustomPacketPayload> {
    private final List<Function<Request<PayloadT>, Boolean>> handlers = new ArrayList<>();

    public Pipeline<PayloadT> add(Function<Request<PayloadT>, Boolean> newHandler) {
        handlers.add(newHandler);
        return this;
    }

    public boolean handle(Request<PayloadT> request) {
        for (Function<Request<PayloadT>, Boolean> h : handlers) {
            if (h.apply(request)) return true;
        }
        return false;
    }
}
