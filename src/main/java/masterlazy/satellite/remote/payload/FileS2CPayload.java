package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.model.FilePayloadType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FileS2CPayload (
        UUID sessionId,
        FilePayloadType payloadType,
        int arg,
        byte[] data
) implements CustomPacketPayload {
    private static final String path = "remote_file_s2c";
    public static final StreamCodec<RegistryFriendlyByteBuf, FileS2CPayload> CODEC =
            StreamCodec.composite(
                    Codecs.UUID,
                    FileS2CPayload::sessionId,
                    Codecs.FILE_COMMAND,
                    FileS2CPayload::payloadType,
                    ByteBufCodecs.INT,
                    FileS2CPayload::arg,
                    Codecs.COMPRESSED_BYTES,
                    FileS2CPayload::data,
                    FileS2CPayload::new
            );
    public static final CustomPacketPayload.Type<FileS2CPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
