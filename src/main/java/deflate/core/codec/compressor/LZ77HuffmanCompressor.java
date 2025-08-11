package deflate.core.codec.compressor;

import deflate.core.codec.header.Header;
import deflate.core.codec.header.HeaderEncoder;
import deflate.core.codec.huffman.HuffmanService;
import deflate.core.codec.lz77.LZ77Service;
import deflate.core.table.DistanceTables;
import deflate.core.table.LengthTables;
import deflate.core.util.BitUtil;

import java.util.HashMap;
import java.util.Map;

public final class LZ77HuffmanCompressor implements Compressor<LZ77HuffmanCompressor.Tuple> {
    LZ77Service lz77Service = new LZ77Service();
    HuffmanService huffmanService = new HuffmanService();

    public class Tuple {
        private Header header;
        private LZ77Service.EncodingResult result;
        private Map<Integer, Long> literalCode;
        private Map<Integer, Long> distanceCode;

        public Tuple(Header header, LZ77Service.EncodingResult result, Map<Integer, Long> literalCode, Map<Integer, Long> distanceCode) {
            this.header = header;
            this.result = result;
            this.literalCode = literalCode;
            this.distanceCode = distanceCode;
        }

        public Header getHeader() {
            return header;
        }

        public LZ77Service.EncodingResult getResult() {
            return result;
        }

        public Map<Integer, Long> getLiteralCode() {
            return literalCode;
        }

        public Map<Integer, Long> getDistanceCode() {
            return distanceCode;
        }
    }

    @Override
    public Tuple compress(byte[] data) {
        //1단계 LZ77
        LZ77Service.EncodingResult compressed = lz77Service.generateCodes(data);

        //2단계 허프만 트리 생성
        Map<Integer, Long> literalLengthFrequency = makeLengthFrequency(compressed);
        Map<Integer, Long> distanceFrequency = makeDistanceFrequency(compressed);
        Map<Integer, Integer> literalCodeLength = huffmanService.buildTreeLengthWithLimit(literalLengthFrequency, 15);
        Map<Integer, Integer> distanceCodeLength = huffmanService.buildTreeLengthWithLimit(distanceFrequency, 15);
        Map<Integer, Long> literalCode = huffmanService.generateCanonicalCodes(literalCodeLength);
        Map<Integer, Long> distanceCode = huffmanService.generateCanonicalCodes(distanceCodeLength);

        int[] literalLengths = new int[286];
        for (int symbol = 0; symbol < 286; symbol++) {
            int add = 0;
            if (symbol <= 255) {
                add = -128;
            }
            Long code = literalCode.get(symbol + add);
            if (code != null) {
                literalLengths[symbol] = Math.toIntExact(BitUtil.extractBits(code).get(1));
            } else {
                literalLengths[symbol] = 0;
            }
        }

        int[] distanceLengths = new int[30];
        for (int symbol = 0; symbol < 30; symbol++) {
            Long code = distanceCode.get(symbol);
            if (code != null) {
                distanceLengths[symbol] = Math.toIntExact(BitUtil.extractBits(code).get(1));
            } else {
                distanceLengths[symbol] = 0;
            }
        }

        //3단계 출력
        HeaderEncoder headerEncoder = new HeaderEncoder();
        Header encodedHeaderInfo = headerEncoder.encodeHeader(0, 2, literalLengths, distanceLengths);
        return new Tuple(encodedHeaderInfo, compressed, literalCode, distanceCode);
    }

    private Map<Integer, Long> makeLengthFrequency(LZ77Service.EncodingResult compressed) {
        int count = compressed.getCount();
        int[] lengths = compressed.getLengths();
        byte[] nextBytes = compressed.getNextBytes();

        Map<Integer, Long> literalLengthFrequency = new HashMap<>();
        for (int i = 0; i < count; i++) {
            int[] code = LengthTables.LENGTH_EQUAL_CODE_BASE_EXTRABIT[lengths[i]];
            literalLengthFrequency.put(code[0], literalLengthFrequency.getOrDefault(code[0], 0L) + 1);
            literalLengthFrequency.put((int) nextBytes[i], literalLengthFrequency.getOrDefault((int) nextBytes[i], 0L) + 1);
        }
        literalLengthFrequency.put(256, 1L);

        return literalLengthFrequency;
    }

    private Map<Integer, Long> makeDistanceFrequency(LZ77Service.EncodingResult compressed) {
        int count = compressed.getCount();
        int[] offsets = compressed.getOffsets();
        Map<Integer, Long> distanceFrequency = new HashMap<>();
        for (int i = 0; i < count; i++) {
            if (offsets[i] > 0) {
                int[] offsetCode = DistanceTables.search(offsets[i]);
                distanceFrequency.put(offsetCode[1], distanceFrequency.getOrDefault(offsetCode[1], 0L) + 1);
            }
        }

        return distanceFrequency;
    }
}
