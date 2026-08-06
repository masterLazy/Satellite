package masterlazy.satellite;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.level.ServerPlayer;

public class SatelliteEvents {
    public static final Event<AllowExecuteCommand> ALLOW_EXECUTE_COMMAND = EventFactory.createArrayBacked(
            AllowExecuteCommand.class, (handlers) -> (player, packet) -> {
        for (AllowExecuteCommand handler : handlers) {
            if (!handler.allowExecuteCommand(player, packet)) {
                return false;
            }
        }
        return true;
    });

    public static final Event<AddingWhitelist> ADDING_WHITELIST = EventFactory.createArrayBacked(
            AddingWhitelist.class, (handlers) -> (ctx, gameProfile) -> {
        for (AddingWhitelist handler : handlers) {
            handler.addingWhitelist(ctx, gameProfile);
        }
    });

    @FunctionalInterface
    public interface AllowExecuteCommand {
        boolean allowExecuteCommand(ServerPlayer player, ServerboundChatCommandPacket packet);
    }

    @FunctionalInterface
    public interface AddingWhitelist {
        void addingWhitelist(CommandSourceStack ctx, GameProfile gameProfile);
    }
}
