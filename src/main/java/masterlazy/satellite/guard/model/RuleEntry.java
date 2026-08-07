package masterlazy.satellite.guard.model;

public record RuleEntry(
    String id,
    String description,
    RuleAction action,
    ConditionEntry[] conditions
) { }
