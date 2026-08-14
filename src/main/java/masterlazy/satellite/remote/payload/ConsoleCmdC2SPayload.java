package masterlazy.satellite.remote.payload;

import masterlazy.satellite.Satellite;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ConsoleCmdC2SPayload (
        String token,
        String command // ConsoleCmdEnum
) implements CustomPacketPayload {
    private static final String path = "remote_console_cmd_c2s";
    public static final StreamCodec<RegistryFriendlyByteBuf, ConsoleCmdC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ConsoleCmdC2SPayload::token,
                    ByteBufCodecs.STRING_UTF8,
                    ConsoleCmdC2SPayload::command,
                    ConsoleCmdC2SPayload::new
            );
    public static final CustomPacketPayload.Type<ConsoleCmdC2SPayload> ID = new CustomPacketPayload.Type<>(Satellite.id(path));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return ID; }
}
