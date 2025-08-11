package deflate.core.codec.compressorwriter;

import deflate.core.io.OutputStream;

import java.io.IOException;

public interface Writer<T> {

    void write(T data, OutputStream out) throws IOException;
}
