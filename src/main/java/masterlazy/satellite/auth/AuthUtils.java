package masterlazy.satellite.auth;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AuthUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    public static String getNewPassword() {
        final String CHAR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final int LENGTH = 8;
        return IntStream.range(0, LENGTH)
                .mapToObj(i -> String.valueOf(CHAR.charAt(RANDOM.nextInt(CHAR.length()))))
                .collect(Collectors.joining());
    }
}
