package deflate.core.io;

import java.io.IOException;

public class InputStream implements AutoCloseable {
    private java.io.InputStream in;
    private int buffer;
    private int bitsInBuffer;

    public InputStream(java.io.InputStream in) {
        this.in = in;
        this.buffer = 0;
        this.bitsInBuffer = 0;
    }

    public int readBit() throws IOException {
        if (bitsInBuffer == 0) {
            buffer = in.read();
            if (buffer == -1) {
                throw new IOException("End of stream reached");
            }
            bitsInBuffer = 8;
        }

        int bit = (buffer >> (bitsInBuffer - 1)) & 1;
        bitsInBuffer--;
        return bit;
    }

    public int readBits(int numBits) throws IOException {
        int result = 0;
        for (int i = 0; i < numBits; i++) {
            result = (result << 1) | readBit();
        }
        return result;
    }

    public byte readByte() throws IOException {
        return (byte) readBits(8);
    }

    // 지정한 개수만큼의 byte 배열을 읽어서 반환하는 메서드
    public byte[] readBytes(int numBytes) throws IOException {
        byte[] result = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            result[i] = readByte();
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
