package masterlazy.satellite.auth.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.SatelliteEvents;
import masterlazy.satellite.auth.AuthService;
import masterlazy.satellite.auth.AuthUtils;
import masterlazy.satellite.auth.command.LoginCommand;
import masterlazy.satellite.auth.command.RegisterCommand;
import masterlazy.satellite.session.SessionService;
import masterlazy.satellite.session.PlayerSession;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class EventHandler {
    private final AuthService service;
    private final SessionService sessionService;

    public EventHandler(AuthService service, SessionService sessionService) {
        this.service = service;
        this.sessionService = sessionService;
    }

    public void register() {
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) ->
            onPlayerJoin(listener.getPlayer()));
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
            onAllowChatMessage(sender.connection.getPlayer()));
        SatelliteEvents.ALLOW_EXECUTE_COMMAND.register(((player, packet) ->
            onAllowExecuteCommand(player, packet.command())));
        SatelliteEvents.ADDING_WHITELIST.register(((ctx, gameProfile) ->
            onAddingWhitelist(ctx.getPlayer(), gameProfile.getName())));
    }

    private void onPlayerJoin(ServerPlayer player) {
        if (Satellite.isSingleGame()) return;
        PlayerSession session = sessionService.getSession(player);
        if (session == null) return;

        session.setLoggedIn(false);
        session.freezePlayer();

        Satellite.sendMessageWithKey(player, "connect.msg");
        if (service.isRegistered(player)) {
            Satellite.sendMessageWithKey(player, "connect.oldUser");
        } else {
            Satellite.sendMessageWithKey(player, "connect.newUser");
        }
        // NOTE: Don't use title command here -> failed to find player
        String title = String.format(Satellite.lang("connect.title"), player.getName().getString());
        Satellite.showTitle(player, title);
    }

    private boolean onAllowChatMessage(ServerPlayer player) {
        if (Satellite.isSingleGame()) return true;
        PlayerSession session = sessionService.getSession(player);
        if (session == null) return false;
        if (session.isLoggedIn()) return true;

        Satellite.sendMessageWithKey(player, "unlogged.msg");
        return false;
    }

    private boolean onAllowExecuteCommand(ServerPlayer player, String command) {
        if (Satellite.isSingleGame()) return true;
        PlayerSession session = sessionService.getSession(player);
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

    private void onAddingWhitelist(ServerPlayer player, String targetName) {
        if (!service.isRegistered(targetName)) {
            String password = AuthUtils.getNewPassword();
            service.savePassword(targetName, password);
            if (player != null) {
                String msg = String.format(Satellite.lang("whitelist.add.pwd"), targetName) + password;
                MutableComponent feedback = Component.literal(msg);
                feedback.setStyle(feedback.getStyle()
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, password))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(Satellite.lang(("pwd.copy"))))));
                Satellite.sendMessage(player, feedback);
            }
            Satellite.LOGGER.info("[Satellite] {} is whitelisted with initial password {}", targetName, password);
        } else {
            if (player != null) {
                Satellite.sendMessageWithKey(player, "whitelist.add.registered");
            }
            Satellite.LOGGER.info("[Satellite] {} is whitelisted. Auto-registration is skipped since they has registered.", targetName);
        }
    }
}
