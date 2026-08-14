package masterlazy.satellite.session;

import masterlazy.satellite.Satellite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

import java.time.Instant;

public class PlayerSession {
    private final ServerPlayer player;

    public PlayerSession(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }


    // ================ Login ================
    private boolean loggedIn;

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }


    // ================ Freeze/unfreeze ================
    private boolean frozen = false;
    private GameType gameMode;
    private float walkingSpeed, flyingSpeed;

    public boolean isFrozen() {
        return frozen;
    }

    public synchronized void freezePlayer() {
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

    public synchronized void restorePlayer() {
        Abilities abilities = player.getAbilities();
        abilities.setWalkingSpeed(walkingSpeed);
        abilities.setFlyingSpeed(flyingSpeed);
        player.setGameMode(gameMode); // Set game mode lastly; if not so will make player unable to sprint.
        Satellite.LOGGER.info("[Satellite] Restored {}", player.getName().getString());
        frozen = false;
    }


    // ================ Request rate ================
    private int requestRate = 0;
    private Instant requestRateResetAt = Instant.now();

    public synchronized boolean tryRequest() {
        Instant now = Instant.now();
        if (requestRateResetAt.isBefore(now)) {
            requestRateResetAt = now.plus(SessionService.REQUEST_RATE_RESET);
            requestRate = 0;
        }
        if (requestRate >= SessionService.REQUEST_RATE_LIMIT) {
            return false;
        }
        requestRate++;
        return true;
    }

    private int authorizeRate = 0;
    private Instant authorizeRateResetAt = Instant.now();

    public synchronized boolean tryAuthorize() {
        Instant now = Instant.now();
        if (authorizeRateResetAt.isBefore(now)) {
            authorizeRateResetAt = now.plus(SessionService.AUTHORIZE_RATE_RESET);
            authorizeRate = 0;
        }
        if (authorizeRate >= SessionService.AUTHORIZE_RATE_LIMIT) {
            return false;
        }
        authorizeRate++;
        return true;
    }
}
