package masterlazy.satellite.guard;

import masterlazy.satellite.HasUuid;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.guard.model.RuleAction;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public class CommandSession implements HasUuid {
    private final UUID uuid;
    private final UUID caller;
    private final String command;
    private final RuleAction action;
    private final Instant expireAt;

    public UUID getUUID() { return uuid; }

    public CommandSession(ServerPlayer caller, String command, RuleAction action, Instant expireAt) {
        this.uuid = UUID.randomUUID();
        this.caller = caller.getUUID();
        this.command = command;
        this.action = action;
        this.expireAt = expireAt;
    }

    public boolean isExpiredWhen(Instant now) {
        return expireAt.isBefore(now);
    }

    public boolean isMatches(UUID caller, String command) {
        return this.caller == caller && this.command.equals(command);
    }

    @Nullable
    public ServerPlayer getCaller() {
        return Satellite.Server.getPlayerList().getPlayer(caller);
    }

    public String getCommand() {
        return command;
    }

    public RuleAction getAction() {
        return action;
    }
}
