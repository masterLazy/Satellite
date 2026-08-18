package masterlazy.satellite.remote;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.text.DecimalFormat;
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

    public static String bytesToString(long bytes) {
        final String[] UNITS = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
        if (bytes <= 0) {
            return "0 B";
        }
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < UNITS.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return DECIMAL_FORMAT.format(size) + " " + UNITS[unitIndex];
    }
}
