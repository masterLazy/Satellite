package masterlazy.satellite.auth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import masterlazy.satellite.auth.handler.CommandHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class RegisterCommand {
    public static final String REGEX = "^register \\S+ \\S+$";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandHandler handler) {
        dispatcher.register(literal("register").then(argument("newPassword", StringArgumentType.word()).then(argument("confirmPassword", StringArgumentType.word()).executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player == null) return 0;
            String password = StringArgumentType.getString(ctx, "newPassword");
            String confirmPassword = StringArgumentType.getString(ctx, "confirmPassword");
            return handler.register(player, password, confirmPassword);
        }))));
    }
}
