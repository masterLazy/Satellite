package masterlazy.satellite.guard.handler;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.SatelliteEvents;
import masterlazy.satellite.guard.GuardService;
import masterlazy.satellite.guard.model.RuleEntry;
import net.minecraft.server.level.ServerPlayer;

public class EventHandler {
    private final GuardService service;

    public EventHandler(GuardService service) {
        this.service = service;
    }

    public void register() {
        SatelliteEvents.ALLOW_EXECUTE_COMMAND.register(((player, packet) ->
                onAllowExecuteCommand(player, packet.command())));
    }

    public boolean onAllowExecuteCommand(ServerPlayer player, String command) {
        if (player == null) return true; // Allow all commands from server
        RuleEntry ruleHit = service.testCommand(command);
        if (ruleHit == null) return true;

        String description = ruleHit.description().isEmpty()? Satellite.lang("misc.empty") : ruleHit.description();
        Satellite.sendMessage(player, String.format(Satellite.lang("guard.cmd.hit"), ruleHit.id(), description));
        switch (ruleHit.action()) {
            case DENY -> { Satellite.sendMessageWithKey(player, "guard.cmd.deny"); }
            case COMFIRM -> { Satellite.sendMessageWithKey(player, "guard.cmd.confirm"); }
            case REQUEST_OP -> { Satellite.sendMessageWithKey(player, "guard.cmd.requestOp"); }
        }
        return false;
    }
}
