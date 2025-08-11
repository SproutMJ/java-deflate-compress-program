package deflate.core.codec;

import deflate.core.codec.compressorfactory.CompressorFactory;
import deflate.core.codec.compressorfactory.LZ77CompressorFactory;
import deflate.core.codec.type.CompressType;

public class CompressorFactoryDetector {

    private final CompressTypeDetector compressTypeDetector;

    public CompressorFactoryDetector(final CompressTypeDetector compressTypeDetector) {
        this.compressTypeDetector = compressTypeDetector;
    }

    public final CompressorFactory createCompressorFactory(byte[] data) {
        CompressType detect = compressTypeDetector.detect(data);
        if(detect == CompressType.DYNAMIC_HUFFMAN) {
            return new LZ77CompressorFactory();
        }

        return null;
    }
}
