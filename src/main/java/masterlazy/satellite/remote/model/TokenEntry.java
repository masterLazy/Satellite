package masterlazy.satellite.remote.model;

import java.time.Instant;

public record TokenEntry(
        String owner,
        Instant expireAt
) { }
