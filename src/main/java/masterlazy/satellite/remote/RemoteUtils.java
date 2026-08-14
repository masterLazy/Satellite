package masterlazy.satellite.remote;

import java.security.SecureRandom;
import java.util.Base64;

public class RemoteUtils {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateToken() {
        final int byteLength = 32;
        byte[] randomBytes = new byte[byteLength];
        RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
