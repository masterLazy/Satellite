package masterlazy.satellite.remote.model;

public enum Status {
    // 2xx
    OK,
    // 4xx
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    TOO_MANY_REQUEST,
    // 5xx
    INTERNAL_SERVER_ERROR,

    UNKNOWN;

    public static Status from(String string) {
        for (Status v : Status.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return UNKNOWN;
    }
}
