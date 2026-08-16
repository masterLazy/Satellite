package masterlazy.satellite.remote.payload;

import masterlazy.satellite.HasRequestId;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.Codecs;
import masterlazy.satellite.remote.model.CommandEnum;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CommandC2SPayload(
        UUID requestId,
        String token,
        CommandEnum command,
        String[] args
) implements CustomPacketPayload, HasRequestId {
    private static final String path = "remote_command_c2s";
    public static final StreamCodec<RegistryFriendlyByteBuf, CommandC2SPayload> CODEC =
            StreamCodec.composite(
                    Codecs.UUID,
                    CommandC2SPayload::requestId,
                    ByteBufCodecs.STRING_UTF8,
                    CommandC2SPayload::token,
                    Codecs.COMMAND,
                    CommandC2SPayload::command,
                    Codecs.STRINGS_UTF8,
                    CommandC2SPayload::args,
                    CommandC2SPayload::new
            );
    public static final CustomPacketPayload.Type<CommandC2SPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
