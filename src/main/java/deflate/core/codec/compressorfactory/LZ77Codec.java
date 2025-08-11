package deflate.core.codec.compressorfactory;

import deflate.core.codec.compressor.LZ77HuffmanCompressor;
import deflate.core.codec.compressorwriter.LZ77Writer;
import deflate.core.io.OutputStream;

import java.io.IOException;

public final class LZ77Codec implements Codec {

    @Override
    public void compressAndWrite(byte[] data, OutputStream out) throws IOException {
        LZ77HuffmanCompressor lz77HuffmanCompressor = new LZ77HuffmanCompressor();
        LZ77Writer lz77Writer = new LZ77Writer();

        lz77Writer.write(lz77HuffmanCompressor.compress(data), out);
    }
}
