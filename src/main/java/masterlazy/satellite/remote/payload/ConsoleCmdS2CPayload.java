package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.CompressedLoad;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ConsoleCmdS2CPayload (
        String result, // RequestResult
        CompressedLoad data
) implements CustomPacketPayload {
    private static final String path = "remote_console_cmd_s2c";
    public static final StreamCodec<RegistryFriendlyByteBuf, ConsoleCmdS2CPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ConsoleCmdS2CPayload::result,
                    CompressedLoad.CODEC,
                    ConsoleCmdS2CPayload::data,
                    ConsoleCmdS2CPayload::new
            );
    public static final CustomPacketPayload.Type<ConsoleCmdS2CPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
