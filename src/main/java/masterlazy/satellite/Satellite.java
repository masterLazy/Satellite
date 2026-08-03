package masterlazy.satellite;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import masterlazy.satellite.auth.AuthJson;
import masterlazy.satellite.auth.AuthManager;
import masterlazy.satellite.command.LoginCommand;
import masterlazy.satellite.command.PasswordCommand;
import masterlazy.satellite.command.RegisterCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class Satellite implements ModInitializer {
    public static final String MOD_ID = "Satellite";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MinecraftServer SERVER;

    public static LangManager langManager = new LangManager();
    public static AuthManager authManager = new AuthManager();
    public static AuthJson authJson = new AuthJson();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVER = server;
        });
        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LoginCommand.register(dispatcher);
            PasswordCommand.register(dispatcher);
            RegisterCommand.register(dispatcher);
            // WhitelistCommand.register(dispatcher); // Use mixin instead
        });

        // Auth
        ServerPlayConnectionEvents.INIT.register((listener, server) -> {
            authManager.onServerPlayConnectionInit(listener.getPlayer());
        });
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            authManager.onServerPlayConnectionJoin(listener.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server)-> {
            authManager.onServerPlayConnectionDisconnect(listener.getPlayer());
        });
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            return authManager.onServerMessageAllowChatMessage(sender.connection.getPlayer());
        });
    }

    // Helpers

    public static String lang(String key) {
        return langManager.get(key);
    }

    public static void sendMessage(ServerPlayer player, Component component) {
        player.sendSystemMessage(component);
    }

    public static void sendMessage(ServerPlayer player, String text) {
        sendMessage(player, Component.literal(text));
    }

    public static void sendMessageWithKey(ServerPlayer player, String key) {
        sendMessage(player, lang(key));
    }

    public static void sendGlobalMessage(String text) {
        PlayerList list = SERVER.getPlayerList();
        list.broadcastSystemMessage(Component.literal(text), false);
    }

    public static void showTitle(ServerPlayer player, String title) {
        Component component = Component.literal(title);
        Function<Component, Packet<?>> f = ClientboundSetTitleTextPacket::new;
        try {
            player.connection.send(f.apply(ComponentUtils.updateForEntity(SERVER.createCommandSourceStack(), component, player, 0)));
        } catch (CommandSyntaxException e) {
            LOGGER.error("[Satellite] Exception occurred when showing title \"{}\" to {}", title, player.getName().getString());
            LOGGER.error(e.toString());
        }
    }

    /**
     * Execute commands as SERVER
     * @note  Commands should NOT start with '/'
     * */
    public static void execute(String command) {
        try {
            SERVER.getCommands().getDispatcher().execute(command, SERVER.createCommandSourceStack());
        } catch (CommandSyntaxException e) {
            LOGGER.error("[Satellite] Exception occurred when executing command \"{}\"", command);
            LOGGER.error(e.toString());
        }
    }

    public static void playNotifySound(ServerPlayer player) {
//        Satellite.execute(String.format("playsound block.note_block.pling master %s %f %f %f 1 0",
//                player.getName().getString(), player.getX(), player.getY(), player.getZ()));
        player.connection.send(new ClientboundSoundPacket(
                SoundEvents.NOTE_BLOCK_PLING,
                SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(),
                1f, 0f, 0));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID.toLowerCase(), path);
    }
}
