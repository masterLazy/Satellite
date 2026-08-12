package masterlazy.satellite.guard.model;

import java.time.Instant;
import java.util.UUID;

public record CommandSession(
        UUID caller,
        String command,
        RuleAction ruleAction,
        Instant expireAt,
        UUID uuid
) { }
