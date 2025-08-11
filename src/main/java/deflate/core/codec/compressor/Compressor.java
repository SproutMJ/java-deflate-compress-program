package deflate.core.codec.compressor;

public interface Compressor<T> {

    T compress(byte[] data);
}
