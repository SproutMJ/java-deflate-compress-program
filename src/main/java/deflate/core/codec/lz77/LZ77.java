package deflate.core.codec.lz77;

import java.util.ArrayList;
import java.util.List;

public class LZ77 {

    private static final int WINDOW_SIZE = 32768;
    private static final int LOOKAHEAD_SIZE = 258;

    public static class Triple {
        int offset;
        int length;
        byte nextByte;

        public Triple(int offset, int length, byte nextByte) {
            this.offset = offset;
            this.nextByte = nextByte;
            this.length = length;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public byte getNextByte() {
            return nextByte;
        }

        @Override
        public String toString() {
            return "(" + offset + ", " + length + ", " + nextByte + ")";
        }
    }

    public List<Triple> generateCodes(byte[] data) {
        List<Triple> compressed = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int matchDistance = 0;
            int matchLength = 0;
            int startIndex = Math.max(0, i - WINDOW_SIZE);

            for (int j = startIndex; j < i; j++) {
                int length = 0;
                while (i + length < data.length && j + length < i &&
                        length < LOOKAHEAD_SIZE && data[j + length] == data[i + length]) {
                    length++;
                }
                if (length > matchLength) {
                    matchLength = length;
                    matchDistance = i - j;
                    if (matchLength == LOOKAHEAD_SIZE) {
                        break;
                    }
                }
            }

            if (matchLength < 3) {
                compressed.add(new Triple(0, 0, data[i]));
                i++;
            } else {
                byte nextByte = (i + matchLength < data.length) ? data[i + matchLength] : 0;
                compressed.add(new Triple(matchDistance, matchLength, nextByte));
                i += matchLength + 1;
            }
        }
        return compressed;
    }

    public byte[] decode(List<Triple> triples) {
        List<Byte> decoded = new ArrayList<>();

        for (int idx = 0; idx < triples.size(); idx++) {
            Triple triple = triples.get(idx);

            if (triple.offset == 0 && triple.length == 0) {
                decoded.add(triple.nextByte);
            } else {
                int start = decoded.size() - triple.offset;
                for (int i = 0; i < triple.length; i++) {
                    decoded.add(decoded.get(start + i));
                }

                //마지막 triple이고 nextByte가 0이면 추가하지 않음
                if (!(idx == triples.size() - 1 && triple.nextByte == 0)) {
                    decoded.add(triple.nextByte);
                }
            }
        }

        // List<Byte>를 byte[]로 변환
        byte[] result = new byte[decoded.size()];
        for (int i = 0; i < decoded.size(); i++) {
            result[i] = decoded.get(i);
        }
        return result;
    }
}
