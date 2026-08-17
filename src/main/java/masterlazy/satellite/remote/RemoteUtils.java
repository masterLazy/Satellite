package masterlazy.satellite.remote;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
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

    public static boolean isSubDirectory(Path parent, Path child) {
        try {
            Path parentReal = parent.toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path childReal = child.toRealPath(LinkOption.NOFOLLOW_LINKS);
            return childReal.startsWith(parentReal) && !childReal.equals(parentReal);
        } catch (IOException e) {
            return false;
        }
    }
}
