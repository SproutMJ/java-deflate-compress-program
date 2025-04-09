package deflate.util;

import java.util.List;

public final class BitUtil {

    private static final int MAX_BITS = 59;
    private static final int LENGTH_BITS = 5;

    public static long init(long value, long length) {
        if (length >= MAX_BITS) {
            throw new IllegalStateException("Maximum bit length reached: " + length);
        }

        length <<= (64 - LENGTH_BITS);
        return value | length;
    }

    public static long addBit(long value, int bit) {
        if (bit != 0 && bit != 1) {
            throw new IllegalArgumentException("Bit must be 0 or 1");
        }

        int length = (int) (value >>> (64 - LENGTH_BITS));

        if (length >= MAX_BITS) {
            throw new IllegalStateException("Maximum bit length reached: " + length);
        }

        long newLength = (long)(length + 1) << (64 - LENGTH_BITS);

        long remainingBits = value & ((1L << (64 - LENGTH_BITS)) - 1);

        return newLength | (remainingBits << 1) | bit;
    }

    public static List<Long> extractBits(long value) {
        int length = (int) (value >>> (64 - LENGTH_BITS));

        long actualBits = value & ((1L << length) - 1);

        return List.of(actualBits, (long) length);
    }
}
