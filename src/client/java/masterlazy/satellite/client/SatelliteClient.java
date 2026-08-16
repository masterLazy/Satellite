package masterlazy.satellite.client;

import masterlazy.satellite.client.remote.RemoteClient;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public class SatelliteClient implements ClientModInitializer {
	public static final RemoteClient remoteClient = new RemoteClient();

	@Override
	public void onInitializeClient() {
		remoteClient.onInitialize();
	}

	public static String getServerName() {
		Minecraft client = Minecraft.getInstance();
		ServerData currentServer = client.getCurrentServer();
		if (currentServer == null) {
			return "single-player";
		}
		return currentServer.name;
	}

	public static String getUserName() {
		Minecraft client = Minecraft.getInstance();
        return client.getUser().getName();
	}
}