package masterlazy.satellite;

public class Config {
    public static final String VERSION = "v1"; // Config version

    public final String version;

    public final MixinConfig mixin = new MixinConfig();
    public final AuthConfig auth = new AuthConfig();
    public final GuardConfig guard = new GuardConfig();
    public final RemoteConfig remote = new RemoteConfig();

    public Config(String version) {
        this.version = version;
    }

    public static class MixinConfig {
        public boolean whiteListCheckName = true;
        public boolean enforceOfflineProfile = true;
    }

    public static class AuthConfig {
        public boolean enabled = true;
        public boolean allowRegister = false;
        public int failureLimitPerMinutes = 5;
    }

    public static class GuardConfig {
        public boolean enabled = true;
        public int confirmTimeoutSeconds = 30;
        public int requestOpTimeoutSeconds = 60;
    }

    public static class RemoteConfig {
        public boolean enabled = true;
        public int sessionInactivityTimeoutMinutes = 30;
        public int requestLimitPerMinute = 1200;
        public final FileTransferConfig fileTransfer = new FileTransferConfig();
    }

    public static class FileTransferConfig {
        public int rateLimitBytesPerSecond = 20 * 1024 * 1024;
        public int partSizeBytes = 128 * 1024;
        public int batchSize = 64;
    }
}