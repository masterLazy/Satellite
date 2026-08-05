package masterlazy.satellite.auth;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.auth.command.LoginCommand;
import masterlazy.satellite.auth.command.RegisterCommand;
import masterlazy.satellite.session.SessionManager;
import masterlazy.satellite.session.model.PlayerSession;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class AuthHandler {
    private final AuthManager authManager;
    private final SessionManager sessionManager;

    public AuthHandler(AuthManager authManager, SessionManager sessionManager) {
        this.authManager = authManager;
        this.sessionManager = sessionManager;
    }

    public void onServerPlayConnectionJoin(ServerPlayer player) {
        if (Satellite.isSingleGame()) return;
        PlayerSession session = sessionManager.getSession(player);
        if (session == null) return;

        session.setLoggedIn(false);
        session.freezePlayer();

        Satellite.sendMessageWithKey(player, "connect.msg");
        if (authManager.isRegistered(player)) {
            Satellite.sendMessageWithKey(player, "connect.oldUser");
        } else {
            Satellite.sendMessageWithKey(player, "connect.newUser");
        }
        // NOTE: Don't use title command here -> failed to find player
        String title = String.format(Satellite.lang("connect.title"), player.getName().getString());
        Satellite.showTitle(player, title);
    }

    public boolean onServerMessageAllowChatMessage(ServerPlayer player) {
        if (Satellite.isSingleGame()) return true;
        PlayerSession session = sessionManager.getSession(player);
        if (session == null) return false;
        if (session.isLoggedIn()) return true;

        Satellite.sendMessageWithKey(player, "unlogged.msg");
        return false;
    }

    public boolean canExecuteCommand(ServerPlayer player, String command) {
        if (Satellite.isSingleGame()) return true;
        PlayerSession session = sessionManager.getSession(player);
        if (session == null) return false;
        if (session.isLoggedIn()) return true;

        if (command.matches(LoginCommand.REGEX) || command.matches(RegisterCommand.REGEX)) return true;
        if (command.startsWith("login") || command.startsWith("register")) {
            Satellite.sendMessageWithKey(player, "unlogged.cmdFriendly");
        } else {
            Satellite.sendMessageWithKey(player, "unlogged.cmd");
        }
        return false;
    }

    // Commands

    public int login(ServerPlayer player, String password) {
        PlayerSession session = sessionManager.getSession(player);
        if (session == null) return 0;
        String username = player.getName().getString();

        if (session.isLoggedIn()) {
            Satellite.sendMessageWithKey(player, "login.logged");
        } else if (!authManager.isRegistered(player)) {
            Satellite.sendMessageWithKey(player, "login.unregistered");
        } else if (!authManager.isCorrectPassword(username, password)) {
            Satellite.sendMessageWithKey(player, "login.incorrectPwd");
        } else {
            session.setLoggedIn(true);
            session.restorePlayer();

            Satellite.sendGlobalMessage(String.format(Satellite.lang("login.success"), username));
            Satellite.LOGGER.info("[Satellite] {} logged in", username);
            Satellite.playNotifySound(player);
        }
        return 1;
    }

    public int register(ServerPlayer player, String password, String confirmPassword) {
        PlayerSession session = sessionManager.getSession(player);
        if (session == null) return 0;
        String username = player.getName().getString();

        if (session.isLoggedIn()) {
            Satellite.sendMessageWithKey(player, "reg.logged");
        } else if (authManager.isRegistered(username)) {
            Satellite.sendMessageWithKey(player, "reg.registered");
        } else if (!password.equals(confirmPassword)) {
            Satellite.sendMessageWithKey(player, "reg.pwdNotMatch");
        } else {
            authManager.savePassword(username, password);
            session.setLoggedIn(true);
            session.restorePlayer();

            Satellite.sendMessageWithKey(player, "reg.success");
            Satellite.sendGlobalMessage(String.format(Satellite.lang("login.success"), username));
            Satellite.LOGGER.info("[Satellite] {} registered and logged in", username);
            Satellite.playNotifySound(player);
        }
        return 1;
    }

    public int changePassword(ServerPlayer player, String oldPassword, String newPassword, String confirmPassword) {
        String username = player.getName().getString();

        if (!authManager.isCorrectPassword(player.getName().getString(), oldPassword)) {
            Satellite.sendMessageWithKey(player, "pwd.change.incorrectPwd");
        } else if (!newPassword.equals(confirmPassword)) {
            Satellite.sendMessageWithKey(player, "pwd.change.pwdNotMatch");
        } else {
            authManager.savePassword(username, newPassword);
            Satellite.sendMessageWithKey(player, "pwd.change.success");
            Satellite.LOGGER.info("[Satellite] {} changed their password.", username);
            Satellite.playNotifySound(player);
        }
        return 1;
    }

    public int resetPassword(ServerPlayer player, String target) {
        if (!Satellite.authManager.isRegistered(target)) {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "pwd.reset.unregistered");
            } else {
                Satellite.LOGGER.error("[Satellite] {} hasn't registered yet", target);
            }
            return 1;
        }
        String password = AuthUtil.getNewPassword();
        Satellite.authManager.savePassword(target, password);
        if (player != null) {
            MutableComponent feedback = Component.literal(Satellite.lang("pwd.reset.success").replace("%s", target) + password);
            feedback.setStyle(feedback.getStyle()
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, password))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Satellite.lang(("pwd.copy"))))));
            Satellite.sendMessage(player, feedback);
        }
        Satellite.LOGGER.info("[Satellite] {}'s password has been reset to: {}", target, password);
        return 1;
    }

    public int reloadPassword(ServerPlayer player) {
        Satellite.authManager.reloadJson();
        if (player != null) {
            Satellite.sendMessageWithKey(player, "pwd.reload.success");
        }
        return 1;
    }

    public int listPassword(ServerPlayer player) {
        StringBuilder msg = new StringBuilder();
        String[] registeredPlayers = Satellite.authManager.getRegisteredPlayerNames();

        // List all registered players
        String registeredListStr = String.join(",", registeredPlayers);
        msg .append(String.format(Satellite.lang("pwd.list.header"), registeredPlayers.length))
                .append(registeredListStr);

        // Warn players in whitelist / op-list but not registered
        PlayerList playerList = Satellite.Server.getPlayerList();
        List<String> warnList = Stream.concat(
                        Arrays.stream(playerList.getWhiteListNames()),
                        Arrays.stream(playerList.getOpNames())
                )
                .distinct()
                .filter(name -> !Satellite.authManager.isRegistered(name))
                .toList();
        if (!warnList.isEmpty()) {
            String warnListStr = String.join(",", warnList);
            msg .append('\n')
                    .append(String.format(Satellite.lang("pwd.list.warn"), warnList.size()))
                    .append(warnListStr);
        }

        if (player != null) {
            Satellite.sendMessage(player, msg.toString());
        } else {
            Satellite.LOGGER.info(msg.toString());
        }
        return 1;
    }
}
