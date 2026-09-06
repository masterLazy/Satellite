package masterlazy.satellite;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

public class HashUtils {
    public static final int BUFFER_SIZE = 8 * 1024 * 1024;

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    });

    private static final ThreadLocal<MessageDigest> MD5 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    });

    private static final ThreadLocal<CRC32> CRC32 = ThreadLocal.withInitial(CRC32::new);

    public static String from(String str, HashAlgo algo) {
        if (algo == HashAlgo.CRC32) {
            CRC32 crc =  CRC32.get();
            crc.reset();
            crc.update(str.getBytes(StandardCharsets.UTF_8));
            return String.format("%08x", crc.getValue());
        } else {
            MessageDigest md;
            if (algo == HashAlgo.MD5) {
                md = MD5.get();
            } else {
                md = SHA256.get();
            }
            md.reset();
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        }
    }

    public static String from(Path file, HashAlgo algo) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        if (algo == HashAlgo.CRC32) {
            CRC32 crc = CRC32.get();
            crc.reset();
            try (InputStream in = Files.newInputStream(file)) {
                while ((bytesRead = in.read(buffer)) != -1) {
                    crc.update(buffer, 0, bytesRead);
                }
            }
            return String.format("%08x", crc.getValue());
        } else {
            MessageDigest md;
            if (algo == HashAlgo.MD5) {
                md = MD5.get();
            } else {
                md = SHA256.get();
            }
            md.reset();
            try (FileInputStream fis = new FileInputStream(file.toString())) {
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        }
    }
}
