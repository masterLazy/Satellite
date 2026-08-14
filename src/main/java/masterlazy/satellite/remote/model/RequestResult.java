package masterlazy.satellite.remote.model;

import org.jetbrains.annotations.Nullable;

public enum RequestResult {
    // 2xx
    OK,
    // 4xx
    BAD_REQUEST,
    UNAUTHORIZED,
    NOT_FOUND,
    CONFLICT,
    TOO_MANY_REQUEST,
    // 5xx
    INTERNAL_SERVER_ERROR;

    @Nullable
    public static RequestResult from(String string) {
        for (RequestResult v : RequestResult.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return null;
    }
}
