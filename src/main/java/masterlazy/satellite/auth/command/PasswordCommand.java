package masterlazy.satellite.auth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import masterlazy.satellite.auth.handler.CommandHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PasswordCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandHandler handler) {
        dispatcher.register(literal("password")
                .then(literal("change")
                .then(argument("oldPassword", StringArgumentType.word())
                .then(argument("newPassword", StringArgumentType.word())
                .then(argument("confirmPassword", StringArgumentType.word())
        .executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player == null) return 0;
            String oldPassword = StringArgumentType.getString(ctx, "oldPassword");
            String newPassword = StringArgumentType.getString(ctx, "newPassword");
            String confirmPassword = StringArgumentType.getString(ctx, "confirmPassword");
            return handler.changePassword(player, oldPassword, newPassword, confirmPassword);
        }))))).then(literal("reset")
                .then(argument("target", StringArgumentType.word())
                .requires(source -> source.hasPermission(3)) // op only
        .executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayer();
            String target = StringArgumentType.getString(ctx, "target");
            return handler.resetPassword(player, target);
        }))).then(literal("reload")
                .requires(source -> source.hasPermission(3)) // op only
        .executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayer();
            return handler.reloadPassword(player);
        })).then(literal("list")
                .requires(source -> source.hasPermission(3)) // op only
        .executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayer();
            return handler.listPassword(player);
        })));

    }
}
