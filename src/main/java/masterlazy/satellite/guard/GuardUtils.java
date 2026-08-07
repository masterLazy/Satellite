package masterlazy.satellite.guard;

import masterlazy.satellite.guard.model.ConditionType;
import masterlazy.satellite.guard.model.RuleAction;
import org.jetbrains.annotations.Nullable;

public class GuardUtils {
    @Nullable
    public static ConditionType conditionTypeOf(String type) {
        for (ConditionType conditionType : ConditionType.values()) {
            if (conditionType.name().equalsIgnoreCase(type)) {
                return conditionType;
            }
        }
        return null;
    }

    @Nullable
    public static RuleAction ruleActionOf(String action) {
        for (RuleAction ruleAction : RuleAction.values()) {
            if (ruleAction.name().equalsIgnoreCase(action)) {
                return ruleAction;
            }
        }
        return null;
    }
}
