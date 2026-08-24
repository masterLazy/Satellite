package masterlazy.satellite.remote;

import masterlazy.satellite.Satellite;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class FileSession {
    private final UUID id;
    private final String pairedToken;
    private final String owner;
    private Instant expireAt;

    private final long size;
    private final FileChannel fileChannel;

    private static final Duration TIMEOUT_INACTIVITY = Duration.ofSeconds(10);

    public FileSession(Path file, String pairedToken, String owner) {
        this.pairedToken = pairedToken;
        this.owner = owner;
        this.id = UUID.randomUUID();
        try {
            size = Files.size(file);
            fileChannel = FileChannel.open(file, StandardOpenOption.READ);
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to start FileSession", e);
            throw new RuntimeException(e);
        }
        refresh();
    }

    public UUID getId() {
        return id;
    }

    public long getSize() {
        return size;
    }

    public FileChannel getFileChannel() {
        return fileChannel;
    }

    public void close() {
        try {
            fileChannel.close();
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to close FileSession", e);
        }
    }

    public boolean checkOwnership(String token, String sender) {
        return pairedToken.equals(token) && owner.equals(sender);
    }

    public void refresh() {
        expireAt = Instant.now().plus(TIMEOUT_INACTIVITY);
    }

    public boolean isExpiredWhen(Instant now) {
        return expireAt.isBefore(now);
    }
}
