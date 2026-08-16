package masterlazy.satellite.client.remote;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("Permission denied. Your token is expired or empty.");
    }
}
