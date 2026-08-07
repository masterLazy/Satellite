package masterlazy.satellite.guard;

import masterlazy.satellite.guard.handler.CommandHandler;
import masterlazy.satellite.guard.handler.EventHandler;
import masterlazy.satellite.guard.model.RuleAction;
import masterlazy.satellite.guard.model.ConditionEntry;
import masterlazy.satellite.guard.model.RuleEntry;
import java.util.ArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuardService {
    private final RuleRepository ruleRepository;
    private final CommandHandler commandHandler;
    private final EventHandler eventHandler;

    public GuardService(String baseDir) {
        ruleRepository = new RuleRepository(baseDir);
        commandHandler = new CommandHandler(this);
        eventHandler = new EventHandler(this);
    }

    public void onInitialize() {
        commandHandler.register();
        eventHandler.register();
    }

    @Nullable
    public RuleEntry getRuleById(String id) {
        for (RuleEntry entry : ruleRepository.getAllEntry()) {
            if (entry.id().equalsIgnoreCase(id)) {
                return entry;
            }
        }
        return null;
    }

    public void insertRule(RuleEntry rule, int priority) {
        ruleRepository.addEntry(rule, Math.min(priority - 1, ruleRepository.getEntryCount()));
        ruleRepository.save();
    }

    public boolean removeRule(RuleEntry rule) {
        if (!ruleRepository.hasNullField(rule)) return false;
        ruleRepository.save();
        return true;
    }

    public RuleEntry[] getRules() {
        return ruleRepository.getAllEntry();
    }

    public void addCondition(RuleEntry rule, ConditionEntry condition) {
        List<ConditionEntry> conditions = new ArrayList<>(List.of(rule.conditions()));
        conditions.add(condition);
        RuleEntry newRule = new RuleEntry(rule.id(), rule.description(), rule.action(), conditions.toArray(ConditionEntry[]::new));
        ruleRepository.replaceEntry(rule, newRule);
        ruleRepository.save();
    }

    public void removeCondition(RuleEntry rule, int no) {
        List<ConditionEntry> conditions = new ArrayList<>(List.of(rule.conditions()));
        conditions.remove(no - 1);
        RuleEntry newRule = new RuleEntry(rule.id(), rule.description(), rule.action(), conditions.toArray(ConditionEntry[]::new));
        ruleRepository.replaceEntry(rule, newRule);
        ruleRepository.save();
    }


    @Nullable
    public RuleEntry testCommand(String command) {
        for (int i = 0; i < ruleRepository.getEntryCount(); i++) {
            RuleEntry ruleSet = ruleRepository.getEntry(i);
            if (isRuleSetHit(ruleSet, command)) {
                if (ruleSet.action() == RuleAction.ALLOW) return null;
                return ruleSet;
            }
        }
        return null;
    }

    private boolean isRuleSetHit(RuleEntry ruleSet, String command) {
        for (ConditionEntry rule : ruleSet.conditions()) {
            if (isRuleHit(rule, command)) return true;
        }
        return false;
    }

    private boolean isRuleHit(ConditionEntry rule, String command) {
        String condition = rule.value();
        switch (rule.type()) {
            case EQUALS -> { return command.equals(condition); }
            case CONTAINS -> { return command.contains(condition); }
            case STARTS_WITH -> { return command.startsWith(condition); }
            case ENDS_WITH -> { return command.endsWith(condition); }
            case MATCHES -> { return command.matches(condition); }
        }
        return false;
    }
}
