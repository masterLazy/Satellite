package masterlazy.satellite.guard;

import masterlazy.satellite.guard.handler.CommandHandler;
import masterlazy.satellite.guard.handler.EventHandler;
import masterlazy.satellite.guard.model.CommandSession;
import masterlazy.satellite.guard.model.ConditionEntry;
import masterlazy.satellite.guard.model.RuleEntry;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuardService {
    private final RuleRepository ruleRepository;
    private final CommandSessionRepository commandSessionRepository;
    private final CommandHandler commandHandler;
    private final EventHandler eventHandler;

    public static final Duration TIMEOUT_CONFIRM = Duration.ofSeconds(30);
    public static final Duration TIMEOUT_REQUEST_OP = Duration.ofSeconds(60);

    public GuardService(String baseDir) {
        ruleRepository = new RuleRepository(baseDir);
        commandSessionRepository = new CommandSessionRepository();
        commandHandler = new CommandHandler(this);
        eventHandler = new EventHandler(this);
    }

    public void onInitialize() {
        commandSessionRepository.register();
        commandHandler.register();
        eventHandler.register();
    }

    @Nullable
    public RuleEntry getRuleById(String id) {
        return ruleRepository.getEntry(id);
    }

    public void insertRule(RuleEntry rule, int priority) {
        ruleRepository.addEntry(rule, Math.min(priority - 1, ruleRepository.getEntryCount()));
        ruleRepository.save();
    }

    public boolean removeRule(RuleEntry rule) {
        if (!ruleRepository.hasEntry(rule)) return false;
        if (!ruleRepository.removeEntry(rule)) return false;
        return ruleRepository.save();
    }

    public RuleEntry[] getRules() {
        return ruleRepository.getAllEntries();
    }

    public boolean replaceRule(RuleEntry oldRule, RuleEntry newRule) {
        if (!ruleRepository.hasEntry(oldRule)) return false;
        ruleRepository.replaceEntry(oldRule, newRule);
        return ruleRepository.save();
    }

    public boolean addCondition(RuleEntry rule, ConditionEntry condition) {
        if (!ruleRepository.hasEntry(rule)) return false;
        List<ConditionEntry> conditions = new ArrayList<>(List.of(rule.conditions()));
        conditions.add(condition);
        RuleEntry newRule = new RuleEntry(rule.id(), rule.description(), rule.action(), conditions.toArray(ConditionEntry[]::new));
        ruleRepository.replaceEntry(rule, newRule);
        return ruleRepository.save();
    }

    public boolean removeCondition(RuleEntry rule, int no) {
        if (!ruleRepository.hasEntry(rule)) return false;
        List<ConditionEntry> conditions = new ArrayList<>(List.of(rule.conditions()));
        conditions.remove(no - 1);
        RuleEntry newRule = new RuleEntry(rule.id(), rule.description(), rule.action(), conditions.toArray(ConditionEntry[]::new));
        ruleRepository.replaceEntry(rule, newRule);
        return ruleRepository.save();
    }


    @Nullable
    public RuleEntry testCommand(String command) {
        for (int i = 0; i < ruleRepository.getEntryCount(); i++) {
            RuleEntry ruleSet = ruleRepository.getEntry(i);
            if (isRuleHit(ruleSet, command)) {
                return ruleSet;
            }
        }
        return null;
    }

    private boolean isRuleHit(RuleEntry ruleSet, String command) {
        for (ConditionEntry rule : ruleSet.conditions()) {
            if (isConditionHit(rule, command)) return true;
        }
        return false;
    }

    private boolean isConditionHit(ConditionEntry rule, String command) {
        String value = rule.value();
        switch (rule.type()) {
            case EQUALS -> { return command.equals(value); }
            case CONTAINS -> { return command.contains(value); }
            case STARTS_WITH -> { return command.startsWith(value); }
            case ENDS_WITH -> { return command.endsWith(value); }
            case MATCHES -> { return command.matches(value); }
        }
        return false;
    }

    public void addCommandSession(CommandSession session) {
        commandSessionRepository.addSession(session);
    }

    public void expireSession(CommandSession session) {
        commandSessionRepository.expireSession(session);
    }

    @Nullable
    public CommandSession getSession(UUID uuid) {
        return commandSessionRepository.getSession(uuid);
    }

    @Nullable
    public CommandSession getSession(ServerPlayer caller, String command) {
        return commandSessionRepository.getSession(caller, command);
    }
}
