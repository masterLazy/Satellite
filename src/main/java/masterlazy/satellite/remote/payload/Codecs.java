package masterlazy.satellite.remote.payload;

import io.netty.buffer.ByteBuf;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
}
