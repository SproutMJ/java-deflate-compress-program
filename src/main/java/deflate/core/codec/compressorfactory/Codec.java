package deflate.core.codec.compressorfactory;

import deflate.core.io.OutputStream;

import java.io.IOException;

public interface Codec {

    void compressAndWrite(byte[] data, OutputStream out) throws IOException;
}
