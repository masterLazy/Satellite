package masterlazy.satellite.auth;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

public class AuthSession {
    private final ServerPlayer player;
    /** NEVER set this directly; use setLoggedIn() instead */
    private boolean loggedIn;

    // State when spawned
    private GameType gameMode;
    private float walkingSpeed, flyingSpeed;

    public AuthSession(ServerPlayer player) {
        this.player = player;
    }

    public boolean isLoggedIn() { return  loggedIn; }

    public void setLoggedIn(boolean loggedIn) {
        if (loggedIn) {
            player.setGameMode(gameMode);

            Abilities abilities = player.getAbilities();
            abilities.setWalkingSpeed(walkingSpeed);
            abilities.setFlyingSpeed(flyingSpeed);
        } else {
            gameMode = player.gameMode.getGameModeForPlayer();
            player.setGameMode(GameType.SPECTATOR);

            Abilities abilities = player.getAbilities();
            walkingSpeed = abilities.getWalkingSpeed();
            flyingSpeed = abilities.getFlyingSpeed();
            abilities.setWalkingSpeed(0.0f);
            abilities.setFlyingSpeed(0.0f);
        }
        this.loggedIn = loggedIn;
    }
}
