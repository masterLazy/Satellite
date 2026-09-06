package masterlazy.satellite.mixin;

import com.mojang.authlib.GameProfile;
import masterlazy.satellite.Satellite;
import net.minecraft.server.players.UserWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(UserWhiteList.class)
public class UserWhiteListMixin {
    /**
     * @author masterLazy
     * @reason Let whitelist check username instead of uuid
     */
    @Overwrite
    public String getKeyForUser(GameProfile gameProfile) {
        if (Satellite.config.mixin.whiteListCheckName) {
            return gameProfile.getName();
        } else {
            return gameProfile.getId().toString();
        }
    }
}
