package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.HasToken;
import masterlazy.satellite.remote.model.FilePayloadType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record FileC2SPayload (
        String token,
        UUID sessionId,
        FilePayloadType payloadType,
        int arg,
        byte[] data
) implements CustomPacketPayload, HasToken {
    private static final String path = "remote_file_c2s";
    public static final StreamCodec<RegistryFriendlyByteBuf, FileC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    FileC2SPayload::token,
                    Codecs.UUID,
                    FileC2SPayload::sessionId,
                    Codecs.FILE_COMMAND,
                    FileC2SPayload::payloadType,
                    ByteBufCodecs.INT,
                    FileC2SPayload::arg,
                    Codecs.COMPRESSED_BYTES,
                    FileC2SPayload::data,
                    FileC2SPayload::new
            );
    public static final CustomPacketPayload.Type<FileC2SPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
