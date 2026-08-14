package masterlazy.satellite.guard.model;

import org.jetbrains.annotations.Nullable;

public enum RuleAction {
    ALLOW,
    DENY,
    CONFIRM,
    REQUEST_OP;

    @Nullable
    public static RuleAction from(String string) {
        for (RuleAction v : RuleAction.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return null;
    }
}
