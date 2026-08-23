package masterlazy.satellite.remote.model;

public enum FilePayloadType {
    TRANSFER,
    FETCH,
    INTERRUPT,
    WAIT,

    UNKNOWN;

    public static FilePayloadType from(String string) {
        for (FilePayloadType v : FilePayloadType.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return UNKNOWN;
    }
}
