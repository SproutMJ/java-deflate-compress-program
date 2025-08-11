package deflate.core.codec;

import deflate.core.codec.type.CompressType;

public final class CompressTypeDetector {
    public CompressType detect(byte[] date) {
        //다이나믹 고정
        return CompressType.DYNAMIC_HUFFMAN;
    }
}
