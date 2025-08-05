package deflate.core.codec.header;

import deflate.core.codec.huffman.Huffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static deflate.core.codec.header.Header.createEncodedHeader;

public class HeaderEncoder {
    public Header encodeHeader(long bfinal, long btype, int[] literalLengths, int[] distanceLengths) {
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
        Huffman huffman = new Huffman();
        Map<Integer, Long> rleFrequency = makeRleFrequency(rleEncoded);
        Map<Integer, Integer> lengths = huffman.buildTreeLengthWithLimit(rleFrequency, 7);
        Map<Integer, Long> codes = huffman.generateCanonicalCodes(lengths);

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

        return createEncodedHeader(bfinal, btype, hlit, hdist, hclen, codeLengths, rleEncoded, codes);
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
