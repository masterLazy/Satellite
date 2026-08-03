package masterlazy.satellite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class RegisterCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("register")
                .then(argument("newPassword", StringArgumentType.word())
                .then(argument("confirmPassword", StringArgumentType.word())
        .executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player == null) return 0;
            AuthSession session = Satellite.authManager.getSession(player);
            if (session == null) {
                Satellite.LOGGER.warn("[Satellite] RegisterCommand failed because session is null");
                return 0;
            }
            String password = StringArgumentType.getString(ctx, "newPassword");
            String username = player.getName().getString();

            if (Satellite.authJson.isRegistered(username)) {
                Satellite.sendMessageWithKey(player, "reg.registered");
            } else if (!password.equals(StringArgumentType.getString(ctx, "confirmPassword"))) {
                Satellite.sendMessageWithKey(player, "reg.pwdNotMatch");
            } else {
                Satellite.authJson.save(username, password);
                session.setLoggedIn(true);
                Satellite.sendMessageWithKey(player, "reg.success");
                Satellite.sendGlobalMessage(String.format(Satellite.lang("login.success"), username));
                Satellite.LOGGER.info("[Satellite] {} registered and logged in", username);
                Satellite.playNotifySound(player);
            }
            return 1;
        }))));
    }
}
