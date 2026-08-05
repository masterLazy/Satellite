package masterlazy.satellite.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(GameProfileCache.class)
public class GameProfileCacheMixin {
    /**
     * @author masterLazy
     * @reason Let always use offline profile
     */
    @Overwrite
    public Optional<GameProfile> get(String string) {
        return Optional.of(UUIDUtil.createOfflineProfile(string));
    }
}
