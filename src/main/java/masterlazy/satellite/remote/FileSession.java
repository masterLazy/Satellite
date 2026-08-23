package masterlazy.satellite.remote;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class FileSession {
    private final UUID id;
    private final Path file;
    private final String pairedToken;
    private final String owner;
    private Instant expireAt;

    private static final Duration TIMEOUT_INACTIVITY = Duration.ofSeconds(10);

    public FileSession(Path file, String pairedToken, String owner) {
        this.file = file;
        this.pairedToken = pairedToken;
        this.owner = owner;
        this.id = UUID.randomUUID();
        refresh();
    }

    public UUID getId() {
        return id;
    }

    public Path getFile() {
        return file;
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
