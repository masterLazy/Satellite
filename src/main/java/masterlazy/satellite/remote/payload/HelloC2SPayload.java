package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record HelloC2SPayload (
        boolean isCompatible
) implements CustomPacketPayload {
    private static final String path = "remote_hello_c2s";
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    HelloC2SPayload::isCompatible,
                    HelloC2SPayload::new
            );
    public static final CustomPacketPayload.Type<HelloC2SPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}