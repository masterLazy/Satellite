package masterlazy.satellite.mixin;

import com.mojang.authlib.GameProfile;
import masterlazy.satellite.Satellite;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(GameProfileCache.class)
public class GameProfileCacheMixin {
    /**
     * @author masterLazy
     * @reason Let always use offline profile
     */
    @Inject(method = "get*", at = @At("RETURN"), cancellable = true)
    public void get(CallbackInfoReturnable<Optional<GameProfile>> cir) {
        GameProfile profile = cir.getReturnValue().orElse(null);
        if (Satellite.config.mixin.enforceOfflineProfile && profile != null) {
            cir.setReturnValue(Optional.of(UUIDUtil.createOfflineProfile(profile.getName())));
        }
    }
}
