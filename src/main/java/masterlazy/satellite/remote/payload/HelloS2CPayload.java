package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record HelloS2CPayload (
        String version
) implements CustomPacketPayload {
    private static final String path = "remote_hello_s2c";
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    HelloS2CPayload::version,
                    HelloS2CPayload::new
            );
    public static final CustomPacketPayload.Type<HelloS2CPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
