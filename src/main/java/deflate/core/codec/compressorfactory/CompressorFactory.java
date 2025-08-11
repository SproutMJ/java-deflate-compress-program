package deflate.core.codec.compressorfactory;

import deflate.core.codec.compressorwriter.Writer;
import deflate.core.codec.compressor.Compressor;

public interface CompressorFactory {

    Compressor getCompressor();
    Writer getWriter();
}
