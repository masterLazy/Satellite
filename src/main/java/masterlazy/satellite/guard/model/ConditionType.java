package masterlazy.satellite.guard.model;

import org.jetbrains.annotations.Nullable;

public enum ConditionType {
    EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    MATCHES;

    @Nullable
    public static ConditionType from(String string) {
        for (ConditionType v : ConditionType.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return null;
    }
}
