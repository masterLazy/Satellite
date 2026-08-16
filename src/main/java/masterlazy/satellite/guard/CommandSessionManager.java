package masterlazy.satellite.guard;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.SessionManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public class CommandSessionManager extends SessionManager<CommandSession> {
    @Override
    protected String getClassName() { return CommandSessionManager.class.getName(); }

    private Instant nextCheck = Instant.now();
    private static final Duration CHECK_BETWEEN = Duration.ofSeconds(1);

    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Instant now = Instant.now();
            if (nextCheck.isAfter(now)) return;
            else nextCheck = now.plus(CHECK_BETWEEN);
            sessionMap.entrySet().removeIf(entry -> {
                CommandSession session = entry.getValue();
                if (session.isExpiredWhen(now)) {
                    ServerPlayer player = session.getCaller();
                    if (player != null) {
                        Satellite.sendMessageWithKey(player, "guard.cmd.expiredYours", session.getCommand());
                    }
                    return true;
                }
                return false;
            });
        });
    }

    @Nullable
    public CommandSession get(ServerPlayer caller, String command) {
        return withReadLock(() -> {
            for (CommandSession session : sessionMap.values()) {
                if (session.isMatches(caller.getUUID(), command)) {
                    return session;
                }
            }
            return null;
        }).orElse(null);
    }
}
