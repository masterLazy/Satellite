package masterlazy.satellite.mixin;

import com.mojang.authlib.GameProfile;
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
        return gameProfile.getName();
    }
}
