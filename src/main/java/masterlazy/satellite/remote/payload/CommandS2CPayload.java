package masterlazy.satellite.remote.payload;

import masterlazy.satellite.remote.HasRequestId;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.model.Status;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CommandS2CPayload(
        UUID requestId,
        Status status,
        String[] results
) implements CustomPacketPayload, HasRequestId {
    private static final String path = "remote_command_s2c";
    public static final StreamCodec<RegistryFriendlyByteBuf, CommandS2CPayload> CODEC =
            StreamCodec.composite(
                    Codecs.UUID,
                    CommandS2CPayload::requestId,
                    Codecs.STATUS,
                    CommandS2CPayload::status,
                    Codecs.STRINGS_UTF8,
                    CommandS2CPayload::results,
                    CommandS2CPayload::new
            );
    public static final CustomPacketPayload.Type<CommandS2CPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
