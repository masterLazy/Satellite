package masterlazy.satellite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import masterlazy.satellite.Satellite;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.ArrayList;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PasswordCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("password")
                .then(literal("change")
                .then(argument("oldPassword", StringArgumentType.word())
                .then(argument("newPassword", StringArgumentType.word())
                .then(argument("confirmPassword", StringArgumentType.word())
        .executes(ctx -> {
            String oldPwd = StringArgumentType.getString(ctx, "oldPassword");
            String newPwd = StringArgumentType.getString(ctx, "newPassword");
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player == null) return 0;
            String username = player.getName().getString();

            if (!Satellite.authJson.isCorrectPassword(player.getName().getString(), oldPwd)) {
                Satellite.sendMessageWithKey(player, "pwd.change.incorrectPwd");
            } else if (!newPwd.equals(StringArgumentType.getString(ctx, "confirmPassword"))) {
                Satellite.sendMessageWithKey(player, "pwd.change.pwdNotMatch");
            } else {
                Satellite.authJson.save(username, newPwd);
                Satellite.sendMessageWithKey(player, "pwd.change.success");
                Satellite.LOGGER.info("[Satellite] {} changed their password.", username);
                Satellite.playNotifySound(player);
            }
            return 1;
            }))))).then(literal("reset")
                .then(argument("target", StringArgumentType.word())
                .requires(source -> source.hasPermission(3)) // op only
        .executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayer();
            String target = StringArgumentType.getString(ctx, "target");

            if (!Satellite.authJson.isRegistered(target)) {
                if (player != null) {
                    Satellite.sendMessageWithKey(player, "pwd.reset.unregistered");
                } else {
                    Satellite.LOGGER.error("[Satellite] {} hasn't registered yet", target);
                }
                return 1;
            }

            String password = Satellite.authManager.generatePassword();
            Satellite.authJson.save(target, password);
            if (player != null) {
                MutableComponent feedback = Component.literal(Satellite.lang("pwd.reset.success").replace("%s", target) + password);
                feedback.setStyle(feedback.getStyle()
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, password))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Satellite.lang(("pwd.copy"))))));
                Satellite.sendMessage(player, feedback);
            }
            Satellite.LOGGER.info("[Satellite] {}'s password has been reset to: {}", target, password);
            return 1;
            }))).then(literal("reload")
                .requires(source -> source.hasPermission(3)) // op only
        .executes(ctx -> {
            Satellite.authJson.read();
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player != null) {
                Satellite.sendMessageWithKey(player, "pwd.reload.success");
            }
            return 1;
            })).then(literal("list")
                .requires(source -> source.hasPermission(3)) // op only
        .executes(ctx -> {
            StringBuilder msg = new StringBuilder();
            ArrayList<String> regList = Satellite.authJson.getPlayers();

            // List all registered players
            msg.append(String.format(Satellite.lang("pwd.list.begin"), regList.size()));
            for (int i = 0; i < regList.size(); i++) {
                msg.append(regList.get(i));
                if (i < regList.size() - 1) {
                    msg.append(',');
                }
            }

            // Warn players in whitelist/op-list but not registered
            PlayerList playerList = ctx.getSource().getServer().getPlayerList();
            ArrayList<String> warnList = new ArrayList<>();
            for (String s : playerList.getWhiteListNames()) {
                if (!regList.contains(s.toLowerCase())) {
                    warnList.add(s);
                }
            }
            for (String s : playerList.getOpNames()) {
                if (!regList.contains(s.toLowerCase())) {
                    warnList.add(s);
                }
            }

            if (!warnList.isEmpty()) {
                msg.append('\n').append(String.format(Satellite.lang("pwd.list.warn"), warnList.size()));
                for (int i = 0; i < warnList.size(); i++) {
                    msg.append(warnList.get(i));
                    if (i < warnList.size() - 1) {
                        msg.append(",");
                    }
                }
            }

            ServerPlayer player = ctx.getSource().getPlayer();
            if (player != null) {
                Satellite.sendMessage(player, msg.toString());
            } else {
                Satellite.LOGGER.info(msg.toString());
            }
            return 1;
        })));

    }
}
