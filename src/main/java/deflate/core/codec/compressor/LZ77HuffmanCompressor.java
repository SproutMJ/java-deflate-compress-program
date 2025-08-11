package deflate.core.codec.compressor;

import deflate.core.codec.header.Header;
import deflate.core.codec.huffman.HuffmanService;
import deflate.core.codec.lz77.LZ77Service;
import deflate.core.table.DistanceTables;
import deflate.core.table.LengthTables;
import deflate.core.util.BitUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LZ77HuffmanCompressor implements Compressor<LZ77HuffmanCompressor.Tuple> {
    LZ77Service lz77Service = new LZ77Service();
    HuffmanService huffmanService = new HuffmanService();

    public class Tuple {
        final private long btype;
        final private int hlit;
        final private int hdist;
        final private int hclen;
        final private int[] codeLengthCodeLengths;
        final private List<Integer> rleEncodedLengths;
        final private Map<Integer, Long> codeLengthCodes;
        final private LZ77Service.EncodingResult result;
        final private Map<Integer, Long> literalCode;
        final private Map<Integer, Long> distanceCode;

        public Tuple(long btype, int hlit, int hdist, int hclen, int[] codeLengthCodeLengths, List<Integer> rleEncodedLengths, Map<Integer, Long> codeLengthCodes, LZ77Service.EncodingResult result, Map<Integer, Long> literalCode, Map<Integer, Long> distanceCode) {
            this.btype = btype;
            this.hlit = hlit;
            this.hdist = hdist;
            this.hclen = hclen;
            this.codeLengthCodeLengths = codeLengthCodeLengths;
            this.rleEncodedLengths = rleEncodedLengths;
            this.codeLengthCodes = codeLengthCodes;
            this.result = result;
            this.literalCode = literalCode;
            this.distanceCode = distanceCode;
        }

        public long getBtype() {
            return btype;
        }

        public int getHlit() {
            return hlit;
        }

        public int getHdist() {
            return hdist;
        }

        public int getHclen() {
            return hclen;
        }

        public int[] getCodeLengthCodeLengths() {
            return codeLengthCodeLengths;
        }

        public List<Integer> getRleEncodedLengths() {
            return rleEncodedLengths;
        }

        public Map<Integer, Long> getCodeLengthCodes() {
            return codeLengthCodes;
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
        // 2. 코드 길이 배열 생성 및 RLE 인코딩
        int literalLength = literalLengths.length;
        for (int i = literalLengths.length - 1; i >= 0; i--) {
            if (literalLengths[i] == 0) {
                literalLength--;
                continue;
            }
            break;
        }

        int distanceLength = distanceLengths.length;
        for (int i = distanceLengths.length - 1; i >= 0; i--) {
            if (distanceLengths[i] == 0) {
                distanceLength--;
                continue;
            }
            break;
        }
        int[] combinedLengths = new int[literalLength + distanceLength];
        for (int i = 0; i < literalLength; i++) {
            combinedLengths[i] = literalLengths[i];
        }
        for (int i = literalLength; i < combinedLengths.length; i++) {
            combinedLengths[i] = distanceLengths[i - literalLength];
        }

        List<Integer> rleEncoded = applyRLE(combinedLengths);

        // 3. 코드 길이 알파벳에 대한 코드 길이 생성
        HuffmanService huffmanService = new HuffmanService();
        Map<Integer, Long> rleFrequency = makeRleFrequency(rleEncoded);
        Map<Integer, Integer> lengths = huffmanService.buildTreeLengthWithLimit(rleFrequency, 7);
        Map<Integer, Long> codes = huffmanService.generateCanonicalCodes(lengths);

        int maxCodeLengthCode = Header.CODE_LENGTH_CODE_ORDER.length;
        for (; maxCodeLengthCode >= 0; maxCodeLengthCode--) {
            if (lengths.containsKey(Header.CODE_LENGTH_CODE_ORDER[maxCodeLengthCode - 1])) {
                break;
            }
        }

        int[] codeLengths = new int[maxCodeLengthCode];
        for (int i = 0; i < maxCodeLengthCode; i++) {
            int symbol = Header.CODE_LENGTH_CODE_ORDER[i];
            codeLengths[i] = lengths.getOrDefault(symbol, 0);
        }

        // HLIT: 사용된 리터럴/길이 코드 수 - 257
        int hlit = literalLength - 257;

        // HDIST: 사용된 거리 코드 수 - 1
        int hdist = distanceLength - 1;

        // HCLEN: 사용된 코드 길이 알파벳 코드 수 - 4
        int hclen = maxCodeLengthCode - 4;
        return new Tuple(2, hlit, hdist, hclen, codeLengths, rleEncoded, codes, compressed, literalCode, distanceCode);
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

    private Map<Integer, Long> makeRleFrequency(List<Integer> rleEncoded) {
        Map<Integer, Long> rleFrequency = new HashMap<>();
        for (int i = 0; i < rleEncoded.size(); i++) {
            int rle = rleEncoded.get(i);
            if (0 <= rle && rle <= 15) {
                rleFrequency.put(rle, rleFrequency.getOrDefault(rle, 0L) + 1);

            } else if (16 <= rle && rle <= 18) {
                rleFrequency.put(rle, rleFrequency.getOrDefault(rle, 0L) + 1);
                i++;
            } else {
                throw new RuntimeException("rle code out of range");
            }
        }

        return rleFrequency;
    }

    private List<Integer> applyRLE(int[] codeLengths) {
        List<Integer> rle = new ArrayList<>();
        int i = 0;

        while (i < codeLengths.length) {
            int currentLen = codeLengths[i];
            int runLength = 1;

            // 같은 길이 개수 세기
            i++;
            while (i < codeLengths.length && codeLengths[i] == currentLen) {
                runLength++;
                i++;
            }

            if (currentLen == 0) {
                // 0 반복 처리
                while (runLength > 0) {
                    if (runLength >= 11) {
                        // 18: 0 반복 (11-138회)
                        int repeatCount = Math.min(runLength, 138);
                        rle.add(18);
                        rle.add(repeatCount - 11);
                        runLength -= repeatCount;
                    } else if (runLength >= 3) {
                        // 17: 0 반복 (3-10회)
                        int repeatCount = Math.min(runLength, 10);
                        rle.add(17);
                        rle.add(repeatCount - 3);
                        runLength -= repeatCount;
                    } else {
                        // 직접 0 추가
                        rle.add(0);
                        runLength--;
                    }
                }
            } else {
                // 첫 번째 길이 직접 추가
                rle.add(currentLen);
                runLength--;

                // 반복 처리
                while (runLength > 0) {
                    if (runLength >= 3) {
                        // 16: 이전 길이 반복 (3-6회)
                        int repeatCount = Math.min(runLength, 6);
                        rle.add(16);
                        rle.add(repeatCount - 3);
                        runLength -= repeatCount;
                    } else {
                        // 직접 길이 추가
                        rle.add(currentLen);
                        runLength--;
                    }
                }
            }
        }

        return rle;
    }
}
