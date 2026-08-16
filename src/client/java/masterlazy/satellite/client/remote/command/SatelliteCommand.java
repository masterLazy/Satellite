package masterlazy.satellite.client.remote.command;

import com.mojang.brigadier.CommandDispatcher;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.client.remote.cli.SshServer;
import masterlazy.satellite.client.remote.RemoteClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SatelliteCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, RemoteClient client, SshServer sshServer) {
        dispatcher.register(literal("satellite").then(literal("status").executes(ctx -> {
            FabricClientCommandSource source = ctx.getSource();
            if (client.isRemoteAvailable()) {
                source.sendFeedback(Component.literal(Satellite.lang("remote.status.available")));
            } else {
                source.sendFeedback(Component.literal(Satellite.lang("remote.status.unavailable")));
            }
            return 1;
        })).then(literal("cli").then(literal("start").executes(ctx -> {
            FabricClientCommandSource source = ctx.getSource();
            int port = sshServer.start();
            if (port != -1) {
                source.sendFeedback(Component.literal(String.format(Satellite.lang("remote.cli.start"), port)));
            } else {
                source.sendFeedback(Component.literal(Satellite.lang("remote.cli.startFailed")));
            }
            return 1;
        })).then(literal("close").executes(ctx -> {
            FabricClientCommandSource source = ctx.getSource();
            if (sshServer.close()) {
                source.sendFeedback(Component.literal(Satellite.lang("remote.cli.close")));
            } else {
                source.sendFeedback(Component.literal(Satellite.lang("remote.cli.closeFailed")));
            }
            return 1;
        })))
        );
    }
}
