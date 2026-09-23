package fuzs.forgeconfigapiport.fabric.impl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Fabric replacement for the NeoForge additions to {@link FriendlyByteBuf} and {@code NeoForgeStreamCodecs} that the
 * vendored network classes rely on.
 */
public final class FriendlyByteBufHelper {
    /**
     * Codec for a byte array of unbounded length, copied from NeoForge's {@code NeoForgeStreamCodecs}.
     */
    public static final StreamCodec<FriendlyByteBuf, byte[]> UNBOUNDED_BYTE_ARRAY = new StreamCodec<>() {
        public byte[] decode(FriendlyByteBuf buf) {
            return buf.readByteArray();
        }

        public void encode(FriendlyByteBuf buf, byte[] data) {
            buf.writeByteArray(data);
        }
    };

    private FriendlyByteBufHelper() {
        // NO-OP
    }
}
