package masterlazy.satellite.auth;

import masterlazy.satellite.HasUuid;
import masterlazy.satellite.RateLimit;
import masterlazy.satellite.Satellite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

import java.time.Duration;
import java.util.UUID;

public class AuthSession implements HasUuid {
    private final UUID playerUUID;
    private boolean loggedIn;

    private boolean frozen;
    private GameType gameMode;
    private float walkingSpeed;
    private float flyingSpeed;

    private ServerPlayer tempPlayer = null;

    private final RateLimit authorizeLimit = new RateLimit(10, Duration.ofSeconds(60));

    public UUID getUUID() { return playerUUID; }

    public AuthSession(ServerPlayer player) {
        playerUUID = player.getUUID();
        loggedIn = false;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void freezePlayer() {
        ServerPlayer player = Satellite.Server.getPlayerList().getPlayer(playerUUID);
        if (player == null) player = tempPlayer;
        if (player == null) {
            Satellite.LOGGER.error("[Satellite] ! Failed to freeze player: cannot find player {}", playerUUID);
            return;
        }
        gameMode = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.SPECTATOR);
        Abilities abilities = player.getAbilities();
        walkingSpeed = abilities.getWalkingSpeed();
        flyingSpeed = abilities.getFlyingSpeed();
        abilities.setWalkingSpeed(0.0f);
        abilities.setFlyingSpeed(0.0f);
        Satellite.LOGGER.info("[Satellite] Froze {}", player.getName().getString());
        frozen = true;
    }

    public void restorePlayer() {
        ServerPlayer player = Satellite.Server.getPlayerList().getPlayer(playerUUID);
        if (player == null) {
            Satellite.LOGGER.info("[Satellite] Failed to restore player because player is null");
            return;
        }
        Abilities abilities = player.getAbilities();
        abilities.setWalkingSpeed(walkingSpeed);
        abilities.setFlyingSpeed(flyingSpeed);
        player.setGameMode(gameMode); // Set game mode lastly; if not so will make player unable to sprint.
        Satellite.LOGGER.info("[Satellite] Restored {}", player.getName().getString());
        frozen = false;
    }

    public boolean tryAuthorize() {
        return authorizeLimit.tryAcquire();
    }

    public void revertAuthorizeRate() {
        authorizeLimit.revertRate();
    }

    public void setTempPlayer(ServerPlayer player) {
        tempPlayer = player;
    }
}
