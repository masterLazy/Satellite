package masterlazy.satellite.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import masterlazy.satellite.Satellite;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** This class is left unused. Don't delete it. */
public class WhitelistCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("whitelist").then(literal("safe-add")
                .then(argument("target", GameProfileArgument.gameProfile())
                .requires(source -> source.hasPermission(3)) // op only
            .executes(ctx -> {
                Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "target");
                UserWhiteList whiteList = Satellite.Server.getPlayerList().getWhiteList();
                ServerPlayer player = ctx.getSource().getPlayer();
                for (GameProfile target : targets) {
                    if (whiteList.isWhiteListed(target)) {
                        if (player != null) {
                            Satellite.sendMessageWithKey(player, "whitelist.add.failed");
                        } else {
                            Satellite.LOGGER.error("[Satellite] {} is already in whitelist", target.getName());
                        }
                        continue;
                    }
                    // Register first
                    String password = Satellite.authManager.generatePassword();
                    Satellite.authJson.save(target.getName(), password);

                    // Add to minecraft whitelist
                    UserWhiteListEntry whiteListEntry = new UserWhiteListEntry(target);
                    whiteList.add(whiteListEntry);

                    if (player != null) {
                        String msg = String.format(Satellite.lang("whitelist.add.pwd"), target.getName()) + password;
                        MutableComponent feedback = Component.literal(msg);
                        feedback.setStyle(feedback.getStyle()
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, password))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Satellite.lang(("pwd.copy"))))));
                        Satellite.sendMessage(player, feedback);
                    }
                    Satellite.LOGGER.info("[Satellite] {} has been whitelisted with initial password {}", target.getName(), password);
                }
                return 1;
            }))));
    }
}
