package masterlazy.satellite.guard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import masterlazy.satellite.SuggestionRule;
import masterlazy.satellite.guard.GuardService;
import masterlazy.satellite.guard.SuggestionAction;
import masterlazy.satellite.guard.SuggestionType;
import masterlazy.satellite.guard.handler.CommandHandler;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GuardCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandHandler handler, GuardService service) {
        dispatcher.register(literal("guard").requires(ctx -> ctx.hasPermission(3)) // op only

                .then(literal("add").then(literal("rule").then(argument("ruleId", StringArgumentType.word()).then(argument("action", StringArgumentType.word()).suggests(new SuggestionAction()).then(argument("priority", IntegerArgumentType.integer(1)).executes(ctx -> {
                            String ruleId = StringArgumentType.getString(ctx, "ruleId");
                            String action = StringArgumentType.getString(ctx, "action");
                            int priority = IntegerArgumentType.getInteger(ctx, "priority");
                            return handler.addRule(ctx.getSource().getPlayer(), ruleId, action, priority, "");
                        })
                        .then(argument("description", StringArgumentType.string()).executes(ctx -> {
                            String ruleId = StringArgumentType.getString(ctx, "ruleId");
                            String action = StringArgumentType.getString(ctx, "action");
                            int priority = IntegerArgumentType.getInteger(ctx, "priority");
                            String description = StringArgumentType.getString(ctx, "description");
                            return handler.addRule(ctx.getSource().getPlayer(), ruleId, action, priority, description);
                        })))))).then(literal("condition").then(argument("ruleId", StringArgumentType.word()).suggests(new SuggestionRule(service)).then(argument("type", StringArgumentType.word()).suggests(new SuggestionType()) // 使用 SuggestionType
                        .then(argument("value", StringArgumentType.string()).executes(ctx -> {
                            String ruleId = StringArgumentType.getString(ctx, "ruleId");
                            String type = StringArgumentType.getString(ctx, "type");
                            String value = StringArgumentType.getString(ctx, "value");
                            return handler.addCondition(ctx.getSource().getPlayer(), ruleId, type, value);
                        }))))))

                .then(literal("remove").then(literal("rule").then(argument("ruleId", StringArgumentType.word()).suggests(new SuggestionRule(service)).executes(ctx -> {
                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                    return handler.removeRule(ctx.getSource().getPlayer(), ruleId);
                }))).then(literal("condition").then(argument("ruleId", StringArgumentType.word()).suggests(new SuggestionRule(service)).then(argument("conditionNo", IntegerArgumentType.integer(1)).executes(ctx -> {
                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                    int conditionNo = IntegerArgumentType.getInteger(ctx, "conditionNo");
                    return handler.removeCondition(ctx.getSource().getPlayer(), ruleId, conditionNo);
                })))))

                .then(literal("set").then(literal("action").then(argument("ruleId", StringArgumentType.word()).suggests(new SuggestionRule(service)).then(argument("action", StringArgumentType.word()).suggests(new SuggestionAction()).executes(ctx -> {
                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                    String action = StringArgumentType.getString(ctx, "action");
                    return handler.setRuleAction(ctx.getSource().getPlayer(), ruleId, action);
                })))).then(literal("priority").then(argument("ruleId", StringArgumentType.word()).suggests(new SuggestionRule(service)).then(argument("priority", IntegerArgumentType.integer(1)).executes(ctx -> {
                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                    int priority = IntegerArgumentType.getInteger(ctx, "priority");
                    return handler.setRulePriority(ctx.getSource().getPlayer(), ruleId, priority);
                })))).then(literal("description").then(argument("ruleId", StringArgumentType.word()).suggests(new SuggestionRule(service)).then(argument("description", StringArgumentType.string()).executes(ctx -> {
                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                    String description = StringArgumentType.getString(ctx, "description");
                    return handler.setRuleDescription(ctx.getSource().getPlayer(), ruleId, description);
                })))))

                .then(literal("list").executes(ctx -> handler.listRules(ctx.getSource().getPlayer())))

                .then(literal("test").then(argument("command", StringArgumentType.string()).executes(ctx -> {
                    String command = StringArgumentType.getString(ctx, "command");
                    return handler.testCommand(ctx.getSource().getPlayer(), command);
                })))

                .then(literal("approve").then(argument("sessionId",StringArgumentType.word()).executes(ctx -> {
                    String uuid = StringArgumentType.getString(ctx, "sessionId");
                    return handler.approveSession(ctx.getSource().getPlayer(), uuid);
                })))
                .then(literal("decline").then(argument("sessionId",StringArgumentType.word()).executes(ctx -> {
                    String uuid = StringArgumentType.getString(ctx, "sessionId");
                    return handler.declineSession(ctx.getSource().getPlayer(), uuid);
                })))
        );
    }
}