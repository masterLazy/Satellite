package masterlazy.satellite.client.remote.command;

import com.mojang.brigadier.CommandDispatcher;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.client.remote.ConsoleSsh;
import masterlazy.satellite.client.remote.RemoteClient;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class RemoteCommand {
    public static void register(CommandDispatcher dispatcher, RemoteClient client, ConsoleSsh consoleSsh) {
        dispatcher.register(literal("remote").then(literal("status").executes(ctx -> {
            FabricClientCommandSource source = ctx.getSource();
            if (client.isRemoteAvailable()) {
                source.sendFeedback(Component.literal(Satellite.lang("remote.status.available")));
            } else {
                source.sendFeedback(Component.literal(Satellite.lang("remote.status.unavailable")));
            }
            return 1;
        })).then(literal("console").then(literal("start").executes(ctx -> {
            FabricClientCommandSource source = ctx.getSource();
            int port = consoleSsh.start();
            if (port != -1) {
                source.sendFeedback(Component.literal(String.format(Satellite.lang("remote.console.start"), port)));
            } else {
                source.sendFeedback(Component.literal(Satellite.lang("remote.console.startFailed")));
            }
            return 1;
        })).then(literal("stop").executes(ctx -> {
            // TODO: 增加反馈
            consoleSsh.stop();
            return 1;
        })))
        );
    }
}
