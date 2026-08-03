package masterlazy.satellite.auth;

import masterlazy.satellite.Satellite;
import net.minecraft.server.level.ServerPlayer;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;

public class AuthManager {
    private final HashMap<UUID, AuthSession> sessions = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    // Event handlers

    public void onServerPlayConnectionInit(ServerPlayer player) {
        String username = player.getName().getString();
        register(player);
        AuthSession session = getSession(player);
        if (session == null) {
            Satellite.LOGGER.error("[Satellite] Failed to register session for {}", username);
        }
    }

    public void onServerPlayConnectionJoin(ServerPlayer player) {
        String username = player.getName().getString();
        AuthSession session = getSession(player);
        if (session == null) return;
        session.setLoggedIn(false);

        Satellite.sendMessageWithKey(player, "connect.msg");
        if (Satellite.authJson.isRegistered(username)) {
            Satellite.sendMessageWithKey(player, "connect.oldUser");
        } else {
            Satellite.sendMessageWithKey(player, "connect.newUser");
        }
        // NOTE: Don't use title command here -> failed to find player
        String title = String.format(Satellite.lang("connect.title"), username);
        Satellite.showTitle(player, title);
    }

    public void onServerPlayConnectionDisconnect(ServerPlayer player) {
        AuthSession session = getSession(player);
        if (session == null) return;
        // Revert the player's profile
        if (!session.isLoggedIn()) { // NOTE: Don't remove this judgement
            session.setLoggedIn(true);
        }
        unregister(player);
    }

    public boolean onServerMessageAllowChatMessage(ServerPlayer player) {
        AuthSession session = getSession(player);
        if (session == null) return false;
        if (session.isLoggedIn()) return true;
        Satellite.sendMessageWithKey(player, "unlogged.msg");
        return false;
    }

    public boolean canExecuteCommand(ServerPlayer player, String command) {
        AuthSession session = getSession(player);
        if (session == null) return false;
        if (session.isLoggedIn()) return true;
        if (command.startsWith("login") || command.startsWith("register")) return true;
        Satellite.sendMessageWithKey(player, "unlogged.cmd");
        return false;
    }

    // Methods

    private void register(ServerPlayer player) {
        UUID uuid = player.getUUID();
        sessions.put(uuid, new AuthSession(player));
    }

    private void unregister(ServerPlayer player) {
        UUID uuid = player.getUUID();
        sessions.remove(uuid);
    }

    // Helpers

    public String generatePassword() {
        final String CHAR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final int LENGTH = 8;
        return IntStream.range(0, LENGTH)
                .mapToObj(i -> String.valueOf(CHAR.charAt(random.nextInt(CHAR.length()))))
                .collect(Collectors.joining());
    }

    @Nullable
    public AuthSession getSession(ServerPlayer player) {
        UUID uuid = player.getUUID();
        return sessions.get(uuid);
    }
}
