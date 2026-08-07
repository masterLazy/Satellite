package masterlazy.satellite.guard.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.guard.GuardService;
import masterlazy.satellite.guard.GuardUtils;
import masterlazy.satellite.guard.command.GuardCommand;
import masterlazy.satellite.guard.model.ConditionEntry;
import masterlazy.satellite.guard.model.ConditionType;
import masterlazy.satellite.guard.model.RuleAction;
import masterlazy.satellite.guard.model.RuleEntry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.level.ServerPlayer;

public class CommandHandler {
    private final GuardService service;

    public CommandHandler(GuardService service) {
        this.service = service;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GuardCommand.register(dispatcher, this, service);
        });
    }

    private void feedbackUnknownRule(ServerPlayer player) {
        if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.unkId");
        else Satellite.LOGGER.error("[Satellite] Given rule ID not exists");
    }

    public int addRule(ServerPlayer player, String ruleId, String action, int priority, String description) {
        RuleAction ruleAction = GuardUtils.ruleActionOf(action);
        if (service.getRuleById(ruleId) != null) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "guard.rule.dupId");
            } else {
                Satellite.LOGGER.error("[Satellite] Given rule ID already exists");
            }
        } else if (ruleAction == null) {
            if (player != null) {
                Satellite.sendMessage(player, String.format(Satellite.lang("misc.unkAtg"), action));
            } else {
                Satellite.LOGGER.error("[Satellite] Unknown rule action \"{}\"", action);
            }
        } else {
            RuleEntry rule = new RuleEntry(ruleId, description, ruleAction, new ConditionEntry[0]); // NEVER set conditions to null
            service.insertRule(rule, priority);
            if (player != null) {
                Satellite.sendMessageWithKey(player, "guard.rule.add");
            }
            Satellite.LOGGER.info("[Satellite] Added rule {} to rule list", ruleId);
        }
        return 1;
    }

    public int removeRule(ServerPlayer player, String ruleId) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else {
            boolean res = service.removeRule(rule);
            if (res) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.add");
                Satellite.LOGGER.info("[Satellite] Removed rule {}", ruleId);
            } else {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.addFailed");
                Satellite.LOGGER.warn("[Satellite] Failed to remove rule {}", ruleId);
            }
        }
        return 1;
    }

    public int addCondition(ServerPlayer player, String ruleId, String type, String value) {
        RuleEntry rule = service.getRuleById(ruleId);
        ConditionType conditionType = GuardUtils.conditionTypeOf(type);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else if (conditionType == null) {
            if (player != null) {
                Satellite.sendMessage(player, String.format(Satellite.lang("misc.unkAtg"), type));
            } else {
                Satellite.LOGGER.error("[Satellite] Unknown condition type \"{}\"", type);
            }
        } else if (value.isEmpty()) {
            if (player != null) {
                Satellite.sendMessage(player, String.format(Satellite.lang("misc.unkAtg"), type));
            } else {
                Satellite.LOGGER.error("[Satellite] Value can't be empty");
            }
        } else {
            ConditionEntry condition = new ConditionEntry(conditionType, value);
            service.addCondition(rule, condition);
            if (player != null) Satellite.sendMessageWithKey(player, "guard.condition.add");
            Satellite.LOGGER.info("[Satellite] Added condition \"{} {}\" ro rule {}", condition.type(), condition.value(), ruleId);
        }
        return 1;
    }

    public int removeCondition(ServerPlayer player, String ruleId, int conditionNo) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else if (rule.conditions().length == 0) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "guard.condition.targetEmpty");
            } else {
                Satellite.LOGGER.error("[Satellite] Target rule has no conditions");
            }
        } else if (conditionNo >= rule.conditions().length) {
            if (player != null) {
                Satellite.sendMessage(player, String.format(Satellite.lang("guard.condition.outOfRange"), 1, rule.conditions().length));
            } else {
                Satellite.LOGGER.error("[Satellite] Condition No. out of range ({}-{})", 1, rule.conditions().length);
            }
        } else {
            service.removeCondition(rule, conditionNo);
            if (player != null) {
                Satellite.sendMessageWithKey(player, "guard.condition.remove");
            }
            ConditionEntry condition = rule.conditions()[conditionNo - 1];
            Satellite.LOGGER.info("[Satellite] Removed condition \"{} {}\" of rule {}", condition.type(), condition.value(), ruleId);
        }
        return 1;
    }

    public int setRuleAction(ServerPlayer player, String ruleId, String action) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        }
        return 1;
    }

    public int setRuleDescription(ServerPlayer player, String ruleId, String description) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        }
        return 1;
    }

    public int setRulePriority(ServerPlayer player, String ruleId, int priority) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        }
        return 1;
    }

    public int listRules(ServerPlayer player) {
        RuleEntry[] rules = service.getRules();
        if (rules.length == 0) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "guard.rule.empty");
            } else {
                Satellite.LOGGER.info("[Satellite] Rule list is empty");
            }
            return 1;
        }
        StringBuilder msg = new StringBuilder();
        String format = "%-8s  %-16s  %-12s  %-10s  %s\n";
        if (player != null) msg.append("§e");
        msg.append(String.format(format, "Priority", "ID", "Action", "Conditions", "Description"));
        if (player != null) msg.append("§f");
        for (int i = 0; i < rules.length; i++) {
            msg.append(String.format(format, i + 1, rules[i].id(), rules[i].action(), rules[i].conditions().length, rules[i].description()));
        }
        if (player != null) {
            Satellite.sendMessage(player, msg.toString());
        } else {
            Satellite.LOGGER.info("[Satellite] Rule list:\n{}", msg);
        }
        return 1;
    }

    public int detailsOf(ServerPlayer player, String ruleId) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else {
            if (rule.conditions().length == 0) {
                if (player != null) {
                    Satellite.sendMessageWithKey(player, "guard.condition.empty");
                } else {
                    Satellite.LOGGER.info("[Satellite] Condition list is empty");
                }
                return 1;
            }
            StringBuilder msg = new StringBuilder();
            String format = "  %-4s  %-10s  %s\n";
            msg.append(String.format(format, "No.", "Type", "Value"));
            for (int i = 0; i < rule.conditions().length; i++) {
                msg.append(String.format(format, i + 1, rule.conditions()[i].type(), rule.conditions()[i].value()));
            }
            if (player != null) {
                Satellite.sendMessage(player, String.format(Satellite.lang("guard.rule.details"), ruleId, rule.description()));
                Satellite.sendMessage(player, msg.toString());
            } else {
                Satellite.LOGGER.info("[Satellite] Details of {}:\nDescription: {}\n{}", ruleId, rule.description(), msg);
            }
        }
        return 1;
    }

    public int testCommand(ServerPlayer player, String command) {
        return 1;
    }
}
