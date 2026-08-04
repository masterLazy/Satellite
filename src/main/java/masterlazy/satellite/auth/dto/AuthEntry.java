package masterlazy.satellite.auth.dto;

public record AuthEntry(
    String name,
    String pwd_hash
) {}

