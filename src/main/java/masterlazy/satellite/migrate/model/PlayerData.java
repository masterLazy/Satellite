package masterlazy.satellite.migrate.model;

import masterlazy.satellite.migrate.MigrateUtils;
import masterlazy.satellite.migrate.UUIDRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record PlayerData (
        @NotNull UUID uuid,
        @Nullable String name,
        @Nullable LocalDate lastJoin
) {
    @Override public String toString() {
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();
        if (lastJoin != null) {
            sb.append(lastJoin.format(FORMATTER));
            sb.append(" ");
        } else {
            sb.append("????-??-?? ");
        }
        if (name != null) {
            sb.append("\033[32m");
        } else {
            sb.append("\033[31m");
        }
        sb.append(uuid);
        sb.append("\033[0m");
        sb.append(" --> ");
        if (name != null) {
            if (MigrateUtils.getOfflineUUID(name).equals(uuid)) {
                sb.append("\033[32mOfflinePlayer:\033[0m");
            } else if (UUIDRepository.getUUID(name).equals(uuid)) {
                sb.append("\033[35mOnlinePlayer: \033[0m");
            }
            sb.append(name);
        } else {
            sb.append("??");
        }
        return sb.toString();
    }

    public static PlayerData merge(PlayerData a, PlayerData b) {
        UUID uuid = a.uuid;
        String name = a.name != null? a.name : b.name;
        LocalDate lastModified;
        if (a.lastJoin != null && b.lastJoin != null) {
            lastModified = a.lastJoin.isAfter(b.lastJoin)? a.lastJoin : b.lastJoin;
        } else if (a.lastJoin != null) {
            lastModified = a.lastJoin;
        } else {
            lastModified = b.lastJoin;
        }
        return new PlayerData(uuid, name, lastModified);
    }
}
