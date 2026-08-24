package masterlazy.satellite.remote.payload;

import io.netty.buffer.ByteBuf;
import masterlazy.satellite.Config;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.FilePayloadType;
import masterlazy.satellite.remote.model.Status;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Codecs {
    // UUID
    public static final StreamCodec<ByteBuf, UUID> UUID = StreamCodec.of((buf, load) -> {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(load.getMostSignificantBits());
        bb.putLong(load.getLeastSignificantBits());
        bb.flip();
        buf.writeBytes(bb);
    }, buf -> {
        ByteBuf bb = buf.readBytes(16);
        return new UUID(bb.getLong(0), bb.getLong(8));
    });

    // Command
    public static final StreamCodec<ByteBuf, CommandEnum> COMMAND = StreamCodec.of((buf, load) -> {
        ByteBufCodecs.STRING_UTF8.encode(buf, load.name());
    }, buf -> {
        String s = ByteBufCodecs.STRING_UTF8.decode(buf);
        return CommandEnum.from(s);
    });

    // Status
    public static final StreamCodec<ByteBuf, Status> STATUS = StreamCodec.of((buf, load) -> {
        ByteBufCodecs.STRING_UTF8.encode(buf, load.name());
    }, buf -> {
        String s = ByteBufCodecs.STRING_UTF8.decode(buf);
        return Status.from(s);
    });

    // FileCommand
    public static final StreamCodec<ByteBuf, FilePayloadType> FILE_COMMAND = StreamCodec.of((buf, load) -> {
        ByteBufCodecs.STRING_UTF8.encode(buf, load.name());
    }, buf -> {
        String s = ByteBufCodecs.STRING_UTF8.decode(buf);
        return FilePayloadType.from(s);
    });

    // String[]
    public static final StreamCodec<ByteBuf, String[]> STRINGS_UTF8 = StreamCodec.of((buf, load) -> {
        int length = load.length;
        buf.writeInt(length);
        for (String s : load) {
            ByteBufCodecs.STRING_UTF8.encode(buf, s);
        }
    }, buf -> {
        int length = buf.readInt();
        String[] res = new String[length];
        for (int i = 0; i < length; i++) {
            res[i] = ByteBufCodecs.STRING_UTF8.decode(buf);
        }
        return res;
    });

    // Compressed byte[]
    public static final StreamCodec<ByteBuf, byte[]> COMPRESSED_BYTES = StreamCodec.of((buf, load) -> {
        try {
            byte[] compressed = Snappy.compress(load);
            int length = compressed.length;
            buf.writeInt(length);
            buf.writeBytes(compressed);
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to compress bytes", e);
            throw new RuntimeException(e);
        }
    }, buf -> {
        try {
            int length = buf.readInt();
            byte[] compressed = new byte[length];
            buf.readBytes(compressed);
            return Snappy.uncompress(compressed);
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to uncompress bytes", e);
            throw new RuntimeException(e);
        }
    });

    // Config
    public static final StreamCodec<ByteBuf, Config> CONFIG = StreamCodec.of((buf, load) -> {
        ByteBufCodecs.STRING_UTF8.encode(buf, Satellite.GSON.toJson(load));
    }, buf -> {
        String s = ByteBufCodecs.STRING_UTF8.decode(buf);
        return Satellite.GSON.fromJson(s, Config.class);
    });
}
