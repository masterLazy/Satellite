package masterlazy.satellite.remote;

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
        Path parentReal = parent.toAbsolutePath().normalize();
        Path childReal = child.toAbsolutePath().normalize();
        return childReal.startsWith(parentReal) && !childReal.equals(parentReal);
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
