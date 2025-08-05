package deflate.core.codec.header;

import deflate.core.codec.huffman.Huffman;
import deflate.core.codec.type.CompressType;
import deflate.core.io.InputStream;
import deflate.core.util.BitUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderDecoder {
    public Header decodeHeader(InputStream bitIn) throws IOException {

        // 1. 블록 헤더 읽기 (3비트)
        // BFINAL (1비트): 마지막 블록 여부
        long bfinal = 0L;
        bfinal = BitUtil.addBit(bfinal, bitIn.readBit());

        // BTYPE (2비트): 압축 방식
        long btype = 0L;
        btype = BitUtil.addBit(btype, bitIn.readBit());
        btype = BitUtil.addBit(btype, bitIn.readBit());

        if (CompressType.NONE.value == btype) {
            //비압축 블록 (BTYPE=00)
            return Header.createDecodedHeaderNoneCompressed(bfinal, btype);
        } else if (CompressType.FIX_HUFFMAN.value == btype) {
            //고정 허프만 코딩 (BTYPE=01)

        } else if (CompressType.DYNAMIC_HUFFMAN.value == btype) {
            // 동적 허프만 코딩(BTYPE=10)인 경우 추가 헤더 필드 읽기
            // 2. HLIT, HDIST, HCLEN 읽기
            // HLIT (5비트): 리터럴/길이 코드 수 - 257
            int hlit = bitIn.readBits(5);

            // HDIST (5비트): 거리 코드 수 - 1
            int hdist = bitIn.readBits(5);

            // HCLEN (4비트): 코드 길이 알파벳 코드 수 - 4
            int hclen = bitIn.readBits(4);

            // 3. 코드 길이 알파벳 코드 길이 읽기 (각 3비트)
            Map<Integer, Integer> codeLengthCodeLengths = new HashMap<>();
            for (int i = 0; i < hclen + 4; i++) {
                int symbol = Header.CODE_LENGTH_CODE_ORDER[i];
                codeLengthCodeLengths.put(symbol, bitIn.readBits(3));
            }

            // 4. 코드 길이 알파벳 허프만 트리 재구성
            Map<Long, Integer> codeLengthAlphabetTree = reconstructReverseHuffmanTree(codeLengthCodeLengths);

            // 5. RLE 압축 해제 및 코드 길이 배열 복원
            List<Integer> decompressedCodeLengths = decompressRLE(
                    bitIn, codeLengthAlphabetTree, (hlit + 257) + (hdist + 1));

            // 리터럴/길이 코드 길이 복사
            Map<Integer, Integer> literalCodeLengths = new HashMap<>();
            for (int i = 0; i < hlit + 257; i++) {
                int add = 0;
                if (i <= 255) {
                    add = -128;
                }
                literalCodeLengths.put(i + add, decompressedCodeLengths.get(i));
            }

            // 거리 코드 길이 복사
            Map<Integer, Integer> distanceCodeLengths = new HashMap<>();
            for (int i = hlit + 257; i < hlit + 257 + hdist + 1; i++) {
                distanceCodeLengths.put(i - (hlit + 257), decompressedCodeLengths.get(i));
            }

            // 7. 리터럴/길이 및 거리 허프만 트리 재구성
            Map<Long, Integer> literalTree = reconstructReverseHuffmanTree(literalCodeLengths);
            Map<Long, Integer> distanceTree = reconstructReverseHuffmanTree(distanceCodeLengths);

            return Header.createDecodedHeaderDynamicCompressed(bfinal, btype, hlit, hdist, hclen, codeLengthCodeLengths, codeLengthAlphabetTree, decompressedCodeLengths, literalCodeLengths, distanceCodeLengths, literalTree, distanceTree);
        }

        throw new RuntimeException("Unrecognized compress type.");
    }

    private Map<Long, Integer> reconstructReverseHuffmanTree(Map<Integer, Integer> codeLengths) {
        Map<Long, Integer> huffmanTree = new HashMap<>();
        Huffman huffman = new Huffman();
        Map<Integer, Long> codes = huffman.generateCanonicalCodes(codeLengths);
        for (Map.Entry<Integer, Long> entry : codes.entrySet()) {
            huffmanTree.put(entry.getValue(), entry.getKey());
        }

        return huffmanTree;
    }

    private List<Integer> decompressRLE(InputStream reader,
                                        Map<Long, Integer> huffmanTree,
                                        int totalCodeLengths) throws IOException {
        List<Integer> codeLengths = new ArrayList<>();
        long currentCode;

        while (codeLengths.size() < totalCodeLengths) {
            currentCode = 0L;

            // 허프만 코드 읽기
            while (true) {
                currentCode = BitUtil.addBit(currentCode, reader.readBit());
                if (huffmanTree.containsKey(currentCode)) {
                    break;
                }
            }

            int symbol = huffmanTree.get(currentCode);

            if (symbol <= 15) {
                // 직접 코드 길이 (0-15)
                codeLengths.add(symbol);
            } else if (symbol == 16) {
                // 이전 길이 반복 (3-6회)
                int repeat = reader.readBits(2) + 3;

                int prevLength = codeLengths.get(codeLengths.size() - 1);
                for (int i = 0; i < repeat; i++) {
                    codeLengths.add(prevLength);
                }
            } else if (symbol == 17) {
                // 0 반복 (3-10회)
                int repeat = reader.readBits(3) + 3;
                for (int i = 0; i < repeat; i++) {
                    codeLengths.add(0);
                }
            } else if (symbol == 18) {
                // 0 반복 (11-138회)
                int repeat = reader.readBits(7) + 11;
                for (int i = 0; i < repeat; i++) {
                    codeLengths.add(0);
                }
            }
        }
        return codeLengths;
    }
}
