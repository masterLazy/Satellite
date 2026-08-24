package masterlazy.satellite;

public record Config (
        int version,
        boolean mixin_whiteListCheckName,
        boolean mixin_enforceOfflineProfile,

        boolean auth_enabled,
        boolean auth_allowRegister,
        int auth_failureLimitPerMinutes,

        boolean guard_enabled,
        int guard_confirmTimeoutSeconds,
        int guard_requestOpTimeoutSeconds,

        boolean remote_enabled,
        int remote_sessionInactivityTimeoutMinutes,
        int remote_requestLimitPerMinute,
        int remote_fileTransferLimitBytesPerSecond,
        int remote_fileTransferPartSizeBytes,
        int remote_fileTransferBatchSize
) {
    public Config() {
        this(   1,
                true,
                true,
                true,
                false,
                5,
                true,
                30,
                60,
                true,
                30,
                1200,
                20*1024*1024,
                128*1024,
                64);
    }
}
