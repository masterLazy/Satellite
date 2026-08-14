package masterlazy.satellite.client;

import masterlazy.satellite.client.remote.RemoteClient;
import net.fabricmc.api.ClientModInitializer;

public class SatelliteClient implements ClientModInitializer {
	private final RemoteClient remoteClient = new RemoteClient();

	@Override
	public void onInitializeClient() {
		remoteClient.onInitialize();
	}
}