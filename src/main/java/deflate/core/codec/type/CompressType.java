package deflate.core.codec.type;

import deflate.core.util.BitUtil;

public enum CompressType {
    NONE(BitUtil.init(0, 2)),
    FIX_HUFFMAN(BitUtil.init(1, 2)),
    DYNAMIC_HUFFMAN(BitUtil.init(2, 2));

    public final long value;
    CompressType(long value) {
        this.value = value;
    }
}
