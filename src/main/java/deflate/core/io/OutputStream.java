package deflate.core.io;

import java.io.Closeable;
import java.io.IOException;

public class OutputStream implements Closeable {
    private java.io.OutputStream out;
    private int currentByte;
    private int numBitsFilled;

    public OutputStream(java.io.OutputStream out) {
        this.out = out;
        this.currentByte = 0;
        this.numBitsFilled = 0;
    }

    public void writeBit(int bit) throws IOException {
        currentByte = (currentByte << 1) | bit;
        numBitsFilled++;
        if (numBitsFilled == 8) {
            out.write(currentByte);
            numBitsFilled = 0;
            currentByte = 0;
        }
    }

    public void writeBit(long value, int length) throws IOException {
        long mask = (1L << (length - 1));
        for (int i = 0; i < length; i++) {
            int bit = (int) (value & mask);
            if (bit == 0) {
                writeBit(0);
            } else {
                writeBit(1);
            }
            mask >>>= 1;
        }
    }

    public void writeByte(long b) throws IOException {
        writeBit((int) b, 8);
    }

    public void writeByte(byte[] bytes) throws IOException {
        for (byte byt : bytes) {
            writeByte(byt);
        }
    }

    private void flush() throws IOException {
        if (numBitsFilled > 0) {
            currentByte <<= (8 - numBitsFilled);
            out.write(currentByte);
        }
        out.flush();
    }

    public void close() throws IOException {
        flush();
        out.close();
    }
}
