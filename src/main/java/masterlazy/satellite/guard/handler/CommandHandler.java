package masterlazy.satellite.guard.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.guard.GuardService;
import masterlazy.satellite.guard.GuardUtils;
import masterlazy.satellite.guard.command.GuardCommand;
import masterlazy.satellite.guard.model.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.util.UUID;

public class CommandHandler {
    private final GuardService service;

    public CommandHandler(GuardService service) {
        this.service = service;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GuardCommand.register(dispatcher, this, service));
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
                Satellite.sendMessageWithKey(player, "misc.unkAtg", action);
            } else {
                Satellite.LOGGER.error("[Satellite] Unknown rule action \"{}\"", action);
            }
        } else {
            RuleEntry rule = new RuleEntry(ruleId, description, ruleAction, new ConditionEntry[0]); // NEVER set conditions to null
            service.insertRule(rule, priority);
            if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.add");
            Satellite.LOGGER.info("[Satellite] Added rule {} to rule list", ruleId);
        }
        return 1;
    }

    public int removeRule(ServerPlayer player, String ruleId) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else {
            if (service.removeRule(rule)) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.remove");
                Satellite.LOGGER.info("[Satellite] Removed rule {}", ruleId);
            } else {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.edit.failed");
                Satellite.LOGGER.error("[Satellite] Failed to remove rule {}", ruleId);
            }
        }
        return 1;
    }

    public int setRuleAction(ServerPlayer player, String ruleId, String action) {
        RuleEntry rule = service.getRuleById(ruleId);
        RuleAction ruleAction = GuardUtils.ruleActionOf(action);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else if (ruleAction == null) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "misc.unkAtg", action);
            } else {
                Satellite.LOGGER.error("[Satellite] Unknown rule action \"{}\"", action);
            }
        } else {
            RuleEntry newRule = new RuleEntry(rule.id(), rule.description(), ruleAction, rule.conditions());
            if (service.replaceRule(rule, newRule)) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.edit");
                Satellite.LOGGER.info("[Satellite] Action of rule {} has been set to: {}", ruleId, action);
            } else {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.edit.fail");
                Satellite.LOGGER.error("[Satellite] Failed to edit action of rule {}", ruleId);
            }
        }
        return 1;
    }

    public int setRuleDescription(ServerPlayer player, String ruleId, String description) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else {
            RuleEntry newRule = new RuleEntry(rule.id(), description, rule.action(), rule.conditions());
            if (service.replaceRule(rule, newRule)) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.edit");
                Satellite.LOGGER.info("[Satellite] Description of rule {} has been set to: {}", ruleId, description);
            } else {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.edit.fail");
                Satellite.LOGGER.error("[Satellite] Failed to edit description of rule {}", ruleId);
            }
        }
        return 1;
    }

    public int setRulePriority(ServerPlayer player, String ruleId, int priority) {
        RuleEntry rule = service.getRuleById(ruleId);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else {
            service.removeRule(rule);
            service.insertRule(rule, priority);
            if (player != null) Satellite.sendMessageWithKey(player, "guard.rule.edit");
            Satellite.LOGGER.info("[Satellite] Priority of rule {} has been set to: {}", ruleId, priority);
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
        MutableComponent feedback = Component.empty();
        if (player != null) feedback.append(Satellite.lang("guard.rule.list.header"));
        else feedback.append(Satellite.lang("guard.rule.listNc.header"));
        feedback.append(Satellite.lang("guard.rule.list.separater"));
        for (int i = 0; i < rules.length; i++) {
            String key = "guard.rule.list.ruleAqua";
            if (player == null) key = "guard.rule.listNc.rule";
            else if (rules[i].action() == RuleAction.ALLOW) key = "guard.rule.list.ruleGreen";
            else if (rules[i].action() == RuleAction.DENY) key = "guard.rule.list.ruleRed";
            feedback.append(String.format(Satellite.lang(key), i+1, rules[i].id(), rules[i].action(), rules[i].description()));
            for (int j = 0; j < rules[i].conditions().length; j++) {
                key = "guard.rule.list.ruleAqua";
                if (player == null) key = "guard.rule.listNc.condition";
                else key = "guard.rule.list.condition";
                feedback.append("\n");
                feedback.append(String.format(Satellite.lang(key), j+1, rules[i].conditions()[j].type(), rules[i].conditions()[j].value()));
            }
            feedback.append(Satellite.lang("guard.rule.list.separater"));
        }
        if (player != null) Satellite.sendMessage(player, feedback);
        else Satellite.LOGGER.info(feedback.getString());
        return 1;
    }

    public int addCondition(ServerPlayer player, String ruleId, String type, String value) {
        RuleEntry rule = service.getRuleById(ruleId);
        ConditionType conditionType = GuardUtils.conditionTypeOf(type);
        if (rule == null) {
            feedbackUnknownRule(player);
        } else if (conditionType == null) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "misc.unkAtg", type);
            } else {
                Satellite.LOGGER.error("[Satellite] Unknown condition type \"{}\"", type);
            }
        } else if (value.isEmpty()) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "misc.unkAtg", type);
            } else {
                Satellite.LOGGER.error("[Satellite] Value can't be empty");
            }
        } else {
            ConditionEntry condition = new ConditionEntry(conditionType, value);
            if (service.addCondition(rule, condition)) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.condition.add");
                Satellite.LOGGER.info("[Satellite] Added condition \"{} {}\" to rule {}", condition.type(), condition.value(), ruleId);
            } else {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.edit.fail");
                Satellite.LOGGER.error("[Satellite] Failed to add condition to rule {}", ruleId);
            }
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
        } else if (conditionNo > rule.conditions().length) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "guard.condition.outOfRange", 1, rule.conditions().length);
            } else {
                Satellite.LOGGER.error("[Satellite] Condition No. out of range ({}-{})", 1, rule.conditions().length);
            }
        } else {
            ConditionEntry condition = rule.conditions()[conditionNo - 1];
            if (service.removeCondition(rule, conditionNo)) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.condition.remove");
                Satellite.LOGGER.info("[Satellite] Removed condition \"{} {}\" of rule {}", condition.type(), condition.value(), ruleId);
            } else {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.edit.fail");
                Satellite.LOGGER.error("[Satellite] Failed to remove condition of rule {}", ruleId);
            }
        }
        return 1;
    }

    public int testCommand(ServerPlayer player, String command) {
        RuleEntry rule = service.testCommand(command);
        if (rule == null) {
            if (player != null) Satellite.sendMessageWithKey(player, "guard.test.pass");
            else Satellite.LOGGER.info("[Satellite] No rule was hit");
        } else {
            if (player != null) {
                String key = "guard.test.hitAqua";
                if (rule.action() == RuleAction.ALLOW) key = "guard.test.hitGreen";
                else if (rule.action() == RuleAction.DENY) key = "guard.test.hitRed";
                Satellite.sendMessageWithKey(player, key, rule.id(), rule.action(), rule.description());
            } else {
                Satellite.LOGGER.info("[Satellite] The command hit rule {}\n  Action: {}\n  Description: {}", rule.id(), rule.action(), rule.description());
            }
        }
        return 1;
    }

    public int approveSession(ServerPlayer player, String uuid) {
        try {
            CommandSession session = service.getSession(UUID.fromString(uuid));
            if (session == null) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.cmd.expired");
                else Satellite.LOGGER.warn("[Satellite] Specific session not found");
            } else {
                service.expireSession(session);
                CommandSession session1 = new CommandSession(session.caller(), session.command(), RuleAction.CONFIRM,
                        Instant.now().plus(service.expireConfirm), UUID.randomUUID());
                service.addCommandSession(session1);
                Satellite.sendMessageWithKey(player, "guard.cmd.approved", session.command(), service.expireConfirm.toSeconds());
            }
        } catch (Exception e) {
            if (player != null) Satellite.sendMessageWithKey(player, "guard.cmd.invalidUUID");
            else Satellite.LOGGER.warn("[Satellite] Invalid UUID string", e);
        }
        return 1;
    }

    public int declineSession(ServerPlayer player, String uuid) {
        try {
            CommandSession session = service.getSession(UUID.fromString(uuid));
            if (session == null) {
                if (player != null) Satellite.sendMessageWithKey(player, "guard.cmd.expired");
                else Satellite.LOGGER.warn("[Satellite] Specific session not found");
            } else {
                service.expireSession(session);
                Satellite.sendMessageWithKey(player, "guard.cmd.declined", session.command());
            }
        } catch (Exception e) {
            if (player != null) Satellite.sendMessageWithKey(player, "guard.cmd.invalidUUID");
            else Satellite.LOGGER.warn("[Satellite] Invalid UUID string", e);
        }
        return 1;
    }
}
