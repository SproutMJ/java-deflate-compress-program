package deflate.core.codec.compressorwriter;

import deflate.core.codec.compressor.LZ77HuffmanCompressor;
import deflate.core.codec.header.Header;
import deflate.core.codec.lz77.LZ77Service;
import deflate.core.io.OutputStream;
import deflate.core.table.DistanceTables;
import deflate.core.table.LengthTables;
import deflate.core.util.BitUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class LZ77Writer implements Writer<LZ77HuffmanCompressor.Tuple> {

    @Override
    public void write(LZ77HuffmanCompressor.Tuple data, OutputStream bitout) throws IOException {
        Header header = data.getHeader();
        LZ77Service.EncodingResult result = data.getResult();
        Map<Integer, Long> literalCode = data.getLiteralCode();
        Map<Integer, Long> distanceCode = data.getDistanceCode();

        bitOutHeader(bitout, header);
        bitOutRle(bitout, header);
        bitOutLZ77(bitout, result, literalCode, distanceCode);
        bitOutEndCode(bitout, literalCode);
    }

    private void bitOutHeader(OutputStream bitOut, Header encodedHeaderInfo) throws IOException {
        bitOut.writeBit(encodedHeaderInfo.getBtype(), 2);
        bitOut.writeBit(encodedHeaderInfo.getHlit(), 5);
        bitOut.writeBit(encodedHeaderInfo.getHdist(), 5);
        bitOut.writeBit(encodedHeaderInfo.getHclen(), 4);

        // 코드 길이 알파벳 코드 길이 출력
        for (int i = 0; i < encodedHeaderInfo.getHclen() + 4; i++) {
            bitOut.writeBit(encodedHeaderInfo.getCodeLengthCodeLengths()[i], 3);
        }
    }

    private void bitOutEndCode(OutputStream bitOut, Map<Integer, Long> literalCode) throws IOException {
        bitOut.writeBit(literalCode.get(256), Math.toIntExact(BitUtil.extractBits(literalCode.get(256)).get(1)));
    }

    private void bitOutLZ77(OutputStream bitOut, LZ77Service.EncodingResult compressed, Map<Integer, Long> literalCode, Map<Integer, Long> distanceCode) throws IOException {
        int count = compressed.getCount();
        int[] offsets = compressed.getOffsets();
        int[] lengths = compressed.getLengths();
        byte[] nextBytes = compressed.getNextBytes();

        for (int i = 0; i < count; i++) {
            if (lengths[i] == 0) {
                bitOut.writeBit(literalCode.get((int) nextBytes[i]), Math.toIntExact(BitUtil.extractBits(literalCode.get((int) nextBytes[i])).get(1)));
            } else {
                int[] codee = LengthTables.LENGTH_EQUAL_CODE_BASE_EXTRABIT[lengths[i]];
                bitOut.writeBit(literalCode.get(codee[0]), Math.toIntExact(BitUtil.extractBits(literalCode.get(codee[0])).get(1)));
                int extraBitCount = codee[2];
                if (extraBitCount > 0) {
                    bitOut.writeBit(lengths[i] - codee[1], extraBitCount);
                }

                int[] offset = DistanceTables.search(offsets[i]);

                bitOut.writeBit(distanceCode.get(offset[1]), Math.toIntExact(BitUtil.extractBits(distanceCode.get(offset[1])).get(1)));
                extraBitCount = offset[2];
                if (extraBitCount > 0) {
                    bitOut.writeBit(offsets[i] - offset[0], extraBitCount);
                }

                bitOut.writeBit(literalCode.get((int) nextBytes[i]), Math.toIntExact(BitUtil.extractBits(literalCode.get((int) nextBytes[i])).get(1)));
            }
        }
    }

    private void bitOutRle(OutputStream bitOut, Header encodedHeaderInfo) throws IOException {
        List<Integer> rleEncoded = encodedHeaderInfo.getRleEncodedLengths();
        Map<Integer, Long> codeLengthCodes = encodedHeaderInfo.getCodeLengthCodes();

        for (int i = 0; i < rleEncoded.size(); i++) {
            int symbol = rleEncoded.get(i);
            Long code = codeLengthCodes.get(symbol);

            // 코드 작성
            bitOut.writeBit(BitUtil.extractBits(code).get(0), Math.toIntExact(BitUtil.extractBits(code).get(1)));

            // 특수 코드의 추가 비트 작성
            if (symbol == 16) {
                // 이전 코드 반복 (3-6회): 2비트 추가
                int repeatCount = rleEncoded.get(++i);
                bitOut.writeBit(repeatCount, 2);
            } else if (symbol == 17) {
                // 0 반복 (3-10회): 3비트 추가
                int repeatCount = rleEncoded.get(++i);
                bitOut.writeBit(repeatCount, 3);
            } else if (symbol == 18) {
                // 0 반복 (11-138회): 7비트 추가
                int repeatCount = rleEncoded.get(++i);
                bitOut.writeBit(repeatCount, 7);
            }
        }
    }
}
