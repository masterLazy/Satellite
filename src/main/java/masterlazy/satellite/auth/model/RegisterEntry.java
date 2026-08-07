package masterlazy.satellite.auth.model;

public record RegisterEntry(
        String name,
        String pwd_hash
) {}
