package deflate.core.codec.compressorfactory;

import deflate.core.codec.compressorwriter.LZ77Writer;
import deflate.core.codec.compressorwriter.Writer;
import deflate.core.codec.compressor.Compressor;
import deflate.core.codec.compressor.LZ77HuffmanCompressor;

public final class LZ77CompressorFactory implements CompressorFactory {


    @Override
    public Compressor<LZ77HuffmanCompressor.Tuple> getCompressor() {
        return new  LZ77HuffmanCompressor();
    }

    @Override
    public Writer<LZ77HuffmanCompressor.Tuple> getWriter() {
        return new LZ77Writer();
    }
}
