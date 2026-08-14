package masterlazy.satellite.guard.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.SatelliteEvents;
import masterlazy.satellite.guard.GuardService;
import masterlazy.satellite.guard.model.CommandSession;
import masterlazy.satellite.guard.model.RuleAction;
import masterlazy.satellite.guard.model.RuleEntry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class EventHandler {
    private final GuardService service;

    public EventHandler(GuardService service) {
        this.service = service;
    }

    public void register() {
        SatelliteEvents.ALLOW_EXECUTE_COMMAND.register(((player, packet) -> onAllowExecuteCommand(player, packet.command())));
    }

    private boolean onAllowExecuteCommand(ServerPlayer player, String command) {
        if (player == null) return true; // Allow all commands from server
        RuleEntry rule = service.testCommand(command);
        if (rule == null || rule.action() == RuleAction.ALLOW) return true;

        CommandSession existedSession = service.getSession(player, command);
        if (existedSession != null && existedSession.ruleAction() == RuleAction.CONFIRM) {
            service.expireSession(existedSession);
            return true;
        }

        String description = rule.description().isEmpty() ? Satellite.lang("misc.empty") : rule.description();
        Satellite.sendMessageWithKey(player, "guard.cmd.hit", rule.id(), description);
        Duration expireDuration = null;
        switch (rule.action()) {
            case DENY -> {
                Satellite.sendMessageWithKey(player, "guard.cmd.deny");
                return false;
            }
            case CONFIRM -> {
                expireDuration = GuardService.TIMEOUT_CONFIRM;
                Satellite.sendMessageWithKey(player, "guard.cmd.confirm", expireDuration.toSeconds());
            }
            case REQUEST_OP -> {
                expireDuration = GuardService.TIMEOUT_REQUEST_OP;
                Satellite.sendMessageWithKey(player, "guard.cmd.requestOp", expireDuration.toSeconds());
            }
        }
        // Set up command existedSession
        if (expireDuration != null) {
            CommandSession session = new CommandSession(player.getUUID(), command, rule.action(), Instant.now().plus(expireDuration), UUID.randomUUID());
            service.addCommandSession(session);
            if (session.ruleAction() == RuleAction.REQUEST_OP) {
                Satellite.EXECUTOR.submit(() -> requestAllOps(session));
            }
        }
        return false;
    }

    private void requestAllOps(CommandSession session) {
        ServerPlayer player = Satellite.Server.getPlayerList().getPlayer(session.caller());
        if (player == null) return;
        MutableComponent feedback = Component.literal(String.format(Satellite.lang("guard.cmd.request.header"),
                player.getName().getString(), session.command()));
        MutableComponent decline = Component.literal(Satellite.lang("guard.cmd.request.decline"));
        decline.setStyle(decline.getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/guard decline " + session.uuid())));
        MutableComponent approve = Component.literal(Satellite.lang("guard.cmd.request.approve"));
        approve.setStyle(approve.getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/guard approve " + session.uuid())));
        feedback.append("\n  ").append(decline).append("        ").append(approve).append("\n").append(Satellite.lang("guard.cmd.request.footer"));

        PlayerList list = Satellite.Server.getPlayerList();
        String[] opNames = list.getOpNames();
        for (String op : opNames) {
            ServerPlayer opPlayer = list.getPlayerByName(op);
            if (opPlayer == null) continue;
            Satellite.sendMessage(opPlayer, feedback);
        }
    }
}
