package deflate;

import deflate.core.codec.CompressTypeDetector;
import deflate.core.codec.compressor.Compressor;
import deflate.core.codec.CompressorFactoryDetector;
import deflate.core.codec.compressorfactory.CompressorFactory;
import deflate.core.codec.compressorwriter.Writer;
import deflate.core.codec.header.Header;
import deflate.core.codec.header.HeaderDecoder;
import deflate.core.codec.lz77.LZ77Service;
import deflate.core.codec.type.CompressType;
import deflate.core.io.InputStream;
import deflate.core.io.OutputStream;
import deflate.core.table.DistanceTables;
import deflate.core.table.LengthTables;
import deflate.core.util.BitUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class Deflate {

    private static final int BUFFER_SIZE = 64 * 1024;

    public void compress(String inputFile, String outputFile) {
        try (FileInputStream fis = new FileInputStream(inputFile);
             OutputStream bitOut = new OutputStream(new FileOutputStream(outputFile, true))) {
            File file = new File(inputFile);
            long fileSize = file.length(); // 파일 전체 크기
            long bytesReadTotal = 0;       // 지금까지 읽은 바이트 수
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            CompressorFactoryDetector compressorFactoryDetector = new CompressorFactoryDetector(new CompressTypeDetector());
            while ((bytesRead = fis.read(buffer)) != -1) {
                bytesReadTotal += bytesRead;
                long bfinal = BitUtil.addBit(0L, 0);
                if (bytesReadTotal == fileSize) {
                    bfinal = BitUtil.addBit(0L, 1);
                }

                if (bytesRead != BUFFER_SIZE) {
                    buffer = Arrays.copyOf(buffer, bytesRead);
                }

                //압축 방식 결정
                CompressorFactory compressorFactory = compressorFactoryDetector.createCompressorFactory(buffer);
                Compressor compressor = compressorFactory.getCompressor();
                Writer writer = compressorFactory.getWriter();

                bitOut.writeBit(bfinal, 1);
                writer.write(compressor.compress(buffer), bitOut);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void decompress(String inputFile, String outputFile) throws IOException {
        try (InputStream bis = new InputStream(new FileInputStream(inputFile));
             FileOutputStream fos = new FileOutputStream(outputFile, true)) {

            boolean lastBlock = false;

            while (!lastBlock) {
                // 헤더 정보 디코딩
                HeaderDecoder headerDecoder = new HeaderDecoder();
                Header decodedHeaderInfo = headerDecoder.decodeHeader(bis);

                lastBlock = BitUtil.extractBits(decodedHeaderInfo.getBfinal()).get(0) == 1;
                long btype = decodedHeaderInfo.getBtype();

                if (CompressType.NONE.value == btype) {
                    //비압축 블록 (BTYPE=00)
                    byte[] bytes = bis.readBytes(BUFFER_SIZE);
                    fos.write(bytes);
                } else if (CompressType.FIX_HUFFMAN.value == btype) {
                    //고정 허프만 코딩 (BTYPE=01)

                } else if (CompressType.DYNAMIC_HUFFMAN.value == btype) {
                    //가변 허프만 코딩 (BTYPE=10)
                    Map<Long, Integer> literalTree = decodedHeaderInfo.getLiteralTree();

                    Map<Long, Integer> distanceTree = decodedHeaderInfo.getDistanceTree();

                    // LZ77 블록 복구 및 원본 파일 복원
                    LZ77Service.EncodingResult result = decompressBlock(bis, literalTree, distanceTree);
                    LZ77Service lz77Service = new LZ77Service();
                    byte[] decode = lz77Service.decode(result);
                    fos.write(decode, 0, decode.length);
                } else {
                    throw new RuntimeException("Unrecognized compress type.");
                }
            }
        }
    }

    private LZ77Service.EncodingResult decompressBlock(InputStream bis,
                                                       Map<Long, Integer> literalTree,
                                                       Map<Long, Integer> distanceTree) throws IOException {
        LZ77Service.EncodingResult encodingResult = new LZ77Service.EncodingResult(64);
        while (true) {
            // 리터럴/길이 코드 읽기
            int symbol = decodeSymbol(literalTree, 15, bis);
            if (symbol == 256) {
                break;
            } else if (symbol < 256) {
                encodingResult.add(0, 0, (byte) symbol);
            } else {
                // 길이-거리 쌍 처리
                int length = decodeLength(symbol, bis);

                // 거리 코드 읽기
                int distSymbol = decodeSymbol(distanceTree, 15, bis);
                int distance = decodeDistance(distSymbol, bis);

                symbol = decodeSymbol(literalTree, 15, bis);

                // 다음 바이트 읽기
                byte nextByte = (byte) symbol;
                encodingResult.add(distance, length, nextByte);
            }
        }

        return encodingResult;
    }

    private int decodeSymbol(Map<Long, Integer> tree, int limit, InputStream bis) throws IOException {
        Integer symbol = null;
        long code = 0;
        while (symbol == null) {
            int bit = bis.readBit();

            code = BitUtil.addBit(code, bit);

            symbol = tree.get(code);

            if (BitUtil.extractBits(code).get(1) > limit) {
                throw new IOException("유효하지 않은 리터럴/길이 코드");
            }
        }

        return symbol;
    }

    private int decodeLength(int symbol, InputStream bis) throws IOException {
        if (symbol < 257 || symbol > 285) {
            throw new IOException("유효하지 않은 길이 심볼: " + symbol);
        }

        int[] lengthEntry = LengthTables.CODE_EQUAL_BASE_EXTRABIT_CODE[symbol - 257];
        int baseLength = lengthEntry[0];
        int extraBits = lengthEntry[1];

        if (extraBits > 0) {
            int extraValue = bis.readBits(extraBits);
            return baseLength + extraValue;
        } else {
            return baseLength;
        }
    }

    private int decodeDistance(int symbol, InputStream bis) throws IOException {
        if (symbol < 0 || symbol > 29) {
            throw new IOException("유효하지 않은 거리 심볼: " + symbol);
        }
        int[] distanceEntry = DistanceTables.CODE_EQUAL_BASE_CODE_EXTRABIT[symbol];
        int baseDistance = distanceEntry[0];
        int extraBits = distanceEntry[2];

        if (extraBits > 0) {
            int extraValue = bis.readBits(extraBits);
            return baseDistance + extraValue;
        }
        return baseDistance;
    }
}
