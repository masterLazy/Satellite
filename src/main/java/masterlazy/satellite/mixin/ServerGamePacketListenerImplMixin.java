package masterlazy.satellite.mixin;

import masterlazy.satellite.SatelliteEvents;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
	public void handleChatCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
		if (!SatelliteEvents.ALLOW_EXECUTE_COMMAND.invoker().allowExecuteCommand(player, packet)) {

			ci.cancel();
		}
	}
}