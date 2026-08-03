package masterlazy.satellite.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import masterlazy.satellite.Satellite;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.WhitelistCommand;

import net.minecraft.server.level.ServerPlayer;
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

        ServerPlayer player = ctx.getPlayer();

        for (GameProfile gameProfile : collection) {
            if (!userWhiteList.isWhiteListed(gameProfile)) {
                if (!Satellite.authJson.isRegistered(gameProfile.getName())) {
                    String password = Satellite.authManager.generatePassword();
                    Satellite.authJson.save(gameProfile.getName(), password);
                    if (player != null) {
                        String msg = String.format(Satellite.lang("whitelist.safe_add.pwd"), gameProfile.getName()) + password;
                        MutableComponent feedback = Component.literal(msg);
                        feedback.setStyle(feedback.getStyle()
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, password))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Satellite.lang(("pwd.copy"))))));
                        Satellite.sendMessage(player, feedback);
                    }
                    Satellite.LOGGER.info("[Satellite] {} is whitelisted with initial password {}", gameProfile.getName(), password);
                } else {
                    if (player != null) {
                        Satellite.sendMessageWithKey(player, "whitelist.safe_add.registered");
                    }
                    Satellite.LOGGER.info("[Satellite] {} is whitelisted. Auto-registration is skipped since they has registered.", gameProfile.getName());
                }

                // Add to whitelist
                UserWhiteListEntry userWhiteListEntry = new UserWhiteListEntry(gameProfile);
                userWhiteList.add(userWhiteListEntry);
                ctx.sendSuccess(() -> Component.translatable("commands.whitelist.add.success", Component.literal(gameProfile.getName())), true);
                i++;
            }
        }

        if (i == 0) {
            throw ERROR_ALREADY_WHITELISTED.create();
        } else {
            return i;
        }
    }
}
