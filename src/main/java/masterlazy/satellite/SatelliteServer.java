package masterlazy.satellite;

import net.fabricmc.api.DedicatedServerModInitializer;

public class SatelliteServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        Satellite.onDedicatedInitialize();
    }
}
