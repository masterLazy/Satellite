package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ConsoleFeedS2CPayload (
        String feedId,
        String parentId,
        String content
) implements CustomPacketPayload {
    private static final String path = "remote_console_feed_s2c";
    public static final StreamCodec<RegistryFriendlyByteBuf, ConsoleFeedS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ConsoleFeedS2CPayload::feedId,
                    ByteBufCodecs.STRING_UTF8,
                    ConsoleFeedS2CPayload::parentId,
                    ByteBufCodecs.STRING_UTF8,
                    ConsoleFeedS2CPayload::content,
                    ConsoleFeedS2CPayload::new
            );
    public static final CustomPacketPayload.Type<ConsoleFeedS2CPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}