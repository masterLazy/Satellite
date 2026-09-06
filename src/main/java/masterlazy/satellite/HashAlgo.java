package masterlazy.satellite;

import org.jetbrains.annotations.Nullable;

public enum HashAlgo {
    CRC32, MD5, SHA256;

    @Nullable
    public static HashAlgo from(String string) {
        for (HashAlgo v : HashAlgo.values()) {
            if (v.name().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return null;
    }
}
