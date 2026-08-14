package masterlazy.satellite.remote;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public record CompressedLoad(byte[] data) {
    public static final StreamCodec<ByteBuf, CompressedLoad> CODEC = StreamCodec.of((buf, load) -> {
        try {
            byte[] compressed = Snappy.compress(load.data);
            buf.writeInt(compressed.length);
            buf.writeBytes(compressed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }, (buf) -> {
        int length = buf.readInt();
        if (buf.readableBytes() < length) {
            throw new RuntimeException("Payload is too short");
        }
        try {
            byte[] raw = Snappy.uncompress(buf.readBytes(length).array());
            return new CompressedLoad(raw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });
}
