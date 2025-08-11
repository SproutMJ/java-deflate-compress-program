package deflate.core.codec;

import deflate.core.codec.compressorfactory.Codec;
import deflate.core.codec.compressorfactory.LZ77Codec;
import deflate.core.codec.type.CompressType;

public class CompressorCodecDetector {

    private final CompressTypeDetector compressTypeDetector;

    public CompressorCodecDetector(final CompressTypeDetector compressTypeDetector) {
        this.compressTypeDetector = compressTypeDetector;
    }

    public final Codec createCompressorCodec(byte[] data) {
        CompressType detect = compressTypeDetector.detect(data);
        if(detect == CompressType.DYNAMIC_HUFFMAN) {
            return new LZ77Codec();
        }

        return null;
    }
}
