package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record AuthorizeS2CPayload (
        int requestId,
        String result, // RequestResult
        String token
) implements CustomPacketPayload {
    private static final String path = "remote_authorize_s2c";
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorizeS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    AuthorizeS2CPayload::requestId,
                    ByteBufCodecs.STRING_UTF8,
                    AuthorizeS2CPayload::result,
                    ByteBufCodecs.STRING_UTF8,
                    AuthorizeS2CPayload::token,
                    AuthorizeS2CPayload::new
            );
    public static final CustomPacketPayload.Type<AuthorizeS2CPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
