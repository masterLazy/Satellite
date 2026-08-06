package masterlazy.satellite.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import masterlazy.satellite.SatelliteEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.WhitelistCommand;

import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {
    @Final
    @Shadow
    private static SimpleCommandExceptionType ERROR_ALREADY_WHITELISTED;

    /**
     * @author masterLazy
     * @reason Add registration logic before add players to whitelist
     */
    @Overwrite
    private static int addPlayers(CommandSourceStack ctx, Collection<GameProfile> collection) throws CommandSyntaxException {
        UserWhiteList userWhiteList = ctx.getServer().getPlayerList().getWhiteList();
        int i = 0;

        for (GameProfile gameProfile : collection) {
            if (userWhiteList.isWhiteListed(gameProfile)) continue;
            SatelliteEvents.ADDING_WHITELIST.invoker().addingWhitelist(ctx, gameProfile);
            // Add to whitelist
            UserWhiteListEntry userWhiteListEntry = new UserWhiteListEntry(gameProfile);
            userWhiteList.add(userWhiteListEntry);
            ctx.sendSuccess(() -> Component.translatable("commands.whitelist.add.success", Component.literal(gameProfile.getName())), true);
            i++;
        }

        if (i == 0) {
            throw ERROR_ALREADY_WHITELISTED.create();
        } else {
            return i;
        }
    }
}
