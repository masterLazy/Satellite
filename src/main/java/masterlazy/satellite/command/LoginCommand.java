package masterlazy.satellite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.AuthSession;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class LoginCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("login")
                .then(argument("password", StringArgumentType.word())
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) return 0;
                AuthSession session = Satellite.authManager.getSession(player);
                if (session == null) {
                    Satellite.LOGGER.warn("[Satellite] LoginCommand failed because session is null");
                    return 0;
                }
                String password = StringArgumentType.getString(ctx, "password");
                String username = player.getName().getString();

                if (session.isLoggedIn()) {
                    Satellite.sendMessageWithKey(player, "login.logged");
                } else if (!Satellite.authJson.isRegistered(username)) {
                    Satellite.sendMessageWithKey(player, "login.unregistered");
                } else if (!Satellite.authJson.isCorrectPassword(username, password)) {
                    Satellite.sendMessageWithKey(player, "login.incorrectPwd");
                } else {
                    session.setLoggedIn(true);
                    Satellite.sendGlobalMessage(String.format(Satellite.lang("login.success"), username));
                    Satellite.LOGGER.info("[Satellite] {} logged in", username);
                    Satellite.playNotifySound(player);
                }
                return 1;
            })));
    }
}
