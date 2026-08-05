package masterlazy.satellite.mixin;

import masterlazy.satellite.Satellite;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
    @Unique
    private boolean satellite$firstCall = true; // We just want to redirect the first call

    /** Redirect the "SERVER IS RUNNING IN OFFLINE/INSECURE MODE" warning */
    @Redirect(method = "initServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;usesAuthentication()Z"))
    boolean usesAuthentication(DedicatedServer instance) {
        if (!satellite$firstCall) return instance.usesAuthentication();
        if (instance.usesAuthentication()) {
            Satellite.LOGGER.warn("Server is running in ONLINE mode with Satellite's authentication system");
        } else {
            if (!instance.isEnforceWhitelist()) {
                Satellite.LOGGER.warn("**** \"enforce-whitelist\" has been set to FALSE. Set to TRUE to ensure security.");
                Satellite.LOGGER.warn("To change this, set \"enforce-whitelist\" to \"true\" in the server.properties file.");
            }
            Satellite.LOGGER.warn("Server is running in OFFLINE mode with Satellite's authentication system");
        }
        satellite$firstCall = false;
        return true;
    }
}
