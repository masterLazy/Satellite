package masterlazy.satellite.remote.pipeline;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.model.Request;
import masterlazy.satellite.remote.payload.HelloC2SPayload;

public class HelloHandler implements PayloadHandler<HelloC2SPayload> {

    @Override
    public boolean handle(Request<HelloC2SPayload> request) {
        HelloC2SPayload payload = request.payload();
        Satellite.B_LOGGER.debug("%s >> HelloC2SPayload:\n%s", request.sender(), Satellite.GSON.toJson(payload));
        if (payload.isCompatible()) {
            Satellite.LOGGER.info("[Satellite] {} connected with a compatible Satellite client", request.sender());
        }
        return true;
    }
}
