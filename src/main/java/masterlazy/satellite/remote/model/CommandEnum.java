package masterlazy.satellite.remote.model;

public enum CommandEnum {
    AUTHORIZE,

    SUBSCRIBE,
    UNSUBSCRIBE,
    FETCH_1000,
    EXECUTE,

    LIST,
    MOVE,
    COPY,
    REMOVE,

    UNKNOWN;

    public static CommandEnum from(String string) {
        for (CommandEnum v : CommandEnum.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return UNKNOWN;
    }
}
