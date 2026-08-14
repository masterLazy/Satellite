package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record AuthorizeC2SPayload (
        int requestId,
        String password
) implements CustomPacketPayload {
    private static final String path = "remote_authorize_c2s";
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorizeC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    AuthorizeC2SPayload::requestId,
                    ByteBufCodecs.STRING_UTF8,
                    AuthorizeC2SPayload::password,
                    AuthorizeC2SPayload::new
            );
    public static final CustomPacketPayload.Type<AuthorizeC2SPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
