package masterlazy.satellite.remote.model;

import org.jetbrains.annotations.Nullable;

public enum ConsoleCmdEnum {
    SUBSCRIBE,
    UNSUBSCRIBE,
    FETCH_1000;

    @Nullable
    public static ConsoleCmdEnum from(String string) {
        for (ConsoleCmdEnum v : ConsoleCmdEnum.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return null;
    }
}
