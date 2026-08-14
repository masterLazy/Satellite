package masterlazy.satellite.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AuthUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    });

    public static String getNewPassword() {
        final String CHAR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final int LENGTH = 8;
        return IntStream.range(0, LENGTH)
                .mapToObj(i -> String.valueOf(CHAR.charAt(RANDOM.nextInt(CHAR.length()))))
                .collect(Collectors.joining());
    }

    public static String getHash(String str) {
        MessageDigest md = SHA256.get();
        md.reset();
        byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
