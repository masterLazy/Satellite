package masterlazy.satellite.session;

import masterlazy.satellite.Satellite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

public class PlayerSession {
    private final ServerPlayer player;

    private boolean loggedIn;
    private boolean froze = false;
    private GameType gameMode;
    private float walkingSpeed, flyingSpeed;

    public PlayerSession(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean isFroze() {
        return froze;
    }

    public void freezePlayer() {
        gameMode = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.SPECTATOR);
        Abilities abilities = player.getAbilities();
        walkingSpeed = abilities.getWalkingSpeed();
        flyingSpeed = abilities.getFlyingSpeed();
        abilities.setWalkingSpeed(0.0f);
        abilities.setFlyingSpeed(0.0f);
        Satellite.LOGGER.info("[Satellite] Froze {}", player.getName().getString());
        froze = true;
    }

    public void restorePlayer() {
        Abilities abilities = player.getAbilities();
        abilities.setWalkingSpeed(walkingSpeed);
        abilities.setFlyingSpeed(flyingSpeed);
        player.setGameMode(gameMode); // Set game mode lastly; if not so will make player unable to sprint.
        Satellite.LOGGER.info("[Satellite] Restored {}", player.getName().getString());
        froze = false;
    }
}
