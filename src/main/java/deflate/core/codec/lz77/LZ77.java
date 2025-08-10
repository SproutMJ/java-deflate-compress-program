package deflate.core.codec.lz77;

import java.util.Arrays;

public class LZ77 {
    private static final int WINDOW_SIZE = 32768;
    private static final int LOOKAHEAD_SIZE = 258;
    private static final int HASH_SIZE = 65536;
    private static final int MIN_MATCH_LENGTH = 3;

    // 해시 머리(head)와 체인 연결(prev)만 사용
    private final int[] hashHead;
    private final int[] prev;

    public LZ77() {
        hashHead = new int[HASH_SIZE];
        prev = new int[WINDOW_SIZE];
        Arrays.fill(hashHead, -1);
        Arrays.fill(prev, -1);
    }

    // 3-바이트 기반 해시 함수
    private int hash3(byte[] data, int pos) {
        if (pos + 2 >= data.length) return 0;
        return ((data[pos] & 0xFF) << 16) |
                ((data[pos + 1] & 0xFF) << 8) |
                (data[pos + 2] & 0xFF);
    }

    // 해시 체인에 위치 삽입 (슬라이딩 윈도우 적용)
    private void insertString(int pos, byte[] data) {
        if (pos + MIN_MATCH_LENGTH - 1 >= data.length) return;
        int hash = hash3(data, pos) & (HASH_SIZE - 1);
        int windowPos = pos & (WINDOW_SIZE - 1);
        prev[windowPos] = hashHead[hash];
        hashHead[hash] = pos;
    }

    // 최장 매치 검색 - 결과는 out[0]=distance, out[1]=length
    private void findLongestMatch(byte[] data, int currentPos, int[] out) {
        int bestLength = 0;
        int bestDistance = 0;
        out[0] = 0;
        out[1] = 0;

        if (currentPos + MIN_MATCH_LENGTH - 1 >= data.length) {
            return;
        }
        int hash = hash3(data, currentPos) & (HASH_SIZE - 1);
        int chainPos = hashHead[hash];
        int maxChain = 256;

        while (chainPos != -1 && maxChain-- > 0) {
            int distance = currentPos - chainPos;
            if (distance <= 0 || distance > WINDOW_SIZE) {
                chainPos = prev[chainPos & (WINDOW_SIZE - 1)];
                continue;
            }

            // 빠른 예비 검사: 현 매치 길이 이후 문자 비교
            int checkIdxChain = chainPos + bestLength;
            int checkIdxCur = currentPos + bestLength;
            if (checkIdxChain >= data.length || checkIdxCur >= data.length
                    || data[checkIdxChain] != data[checkIdxCur]) {
                chainPos = prev[chainPos & (WINDOW_SIZE - 1)];
                continue;
            }

            // 실제 매칭 길이 계산
            int matchLength = getMatchLength(data, chainPos, currentPos);
            if (matchLength > bestLength) {
                bestLength = matchLength;
                bestDistance = distance;
                if (bestLength >= LOOKAHEAD_SIZE) break;
            }
            chainPos = prev[chainPos & (WINDOW_SIZE - 1)];
        }
        out[0] = bestDistance;
        out[1] = bestLength;
    }

    // 8바이트 단위 비교로 매치 길이 계산
    private int getMatchLength(byte[] data, int pos1, int pos2) {
        int maxLength = Math.min(LOOKAHEAD_SIZE, data.length - pos2);
        // also ensure pos1 has enough bytes to compare up to maxLength
        maxLength = Math.min(maxLength, data.length - pos1);
        int length = 0;
        while (length + 8 <= maxLength) {
            long word1 = getLong(data, pos1 + length);
            long word2 = getLong(data, pos2 + length);
            if (word1 != word2) {
                // 일치가 깨지는 바이트 위치 찾기
                while (length < maxLength && data[pos1 + length] == data[pos2 + length]) {
                    length++;
                }
                return length;
            }
            length += 8;
        }
        while (length < maxLength && data[pos1 + length] == data[pos2 + length]) {
            length++;
        }
        return length;
    }

    private long getLong(byte[] data, int pos) {
        // 바운더리 체크 후 8바이트 리틀엔디언 읽기
        if (pos + 8 > data.length) {
            long result = 0;
            for (int i = 0; i < data.length - pos; i++) {
                result |= ((long)(data[pos + i] & 0xFF)) << (8 * i);
            }
            return result;
        }
        return ((long)(data[pos] & 0xFF))       |
                ((long)(data[pos+1] & 0xFF) << 8) |
                ((long)(data[pos+2] & 0xFF) << 16)|
                ((long)(data[pos+3] & 0xFF) << 24)|
                ((long)(data[pos+4] & 0xFF) << 32)|
                ((long)(data[pos+5] & 0xFF) << 40)|
                ((long)(data[pos+6] & 0xFF) << 48)|
                ((long)(data[pos+7] & 0xFF) << 56);
    }

    public static class EncodingResult {
        private int count;
        private int[] offsets;
        private int[] lengths;
        private byte[] nextBytes;

        public EncodingResult(int initialCapacity) {
            offsets = new int[initialCapacity];
            lengths = new int[initialCapacity];
            nextBytes = new byte[initialCapacity];
            count = 0;
        }

        private void ensureCapacity(int need) {
            if (need <= offsets.length) return;
            int newCap = offsets.length;
            while (newCap < need) newCap <<= 1;
            offsets = Arrays.copyOf(offsets, newCap);
            lengths = Arrays.copyOf(lengths, newCap);
            nextBytes = Arrays.copyOf(nextBytes, newCap);
        }

        public void add(int offset, int length, byte next) {
            ensureCapacity(count + 1);
            offsets[count] = offset;
            lengths[count] = length;
            nextBytes[count] = next;
            count++;
        }

        public int getCount() {
            return count;
        }

        public int[] getOffsets() {
            return offsets;
        }

        public int[] getLengths() {
            return lengths;
        }

        public byte[] getNextBytes() {
            return nextBytes;
        }
    }

    public EncodingResult generateCodes(byte[] data) {
        int n = data.length;
        EncodingResult compressed = new EncodingResult(Math.max(64, n / 2));

        Arrays.fill(hashHead, -1);
        Arrays.fill(prev, -1);

        int[] matchOut = new int[2]; // out[0]=distance, out[1]=length
        int i = 0;
        while (i < n) {
            findLongestMatch(data, i, matchOut);
            int distance = matchOut[0];
            int length = matchOut[1];

            if (length < MIN_MATCH_LENGTH) {
                // 리터럴
                compressed.add(0, 0, data[i]);
                insertString(i, data);
                i++;
            } else {
                // 매치 (offset, length, nextByte)
                byte nextByte = (i + length < n) ? data[i + length] : 0;
                compressed.add(distance, length, nextByte);
                // 매치된 모든 위치 삽입
                for (int j = 0; j <= length; j++) {
                    insertString(i + j, data);
                }
                i += length + 1;
            }
        }
        return compressed;
    }

    public byte[] decode(EncodingResult enc) {
        int count = enc.getCount();
        int[] offsets = enc.getOffsets();
        int[] lengths = enc.getLengths();
        byte[] nextBytes = enc.getNextBytes();

        int cap = Math.max(1024, count * 4 + 16);
        byte[] out = new byte[cap];
        int outPos = 0;

        for (int idx = 0; idx < count; idx++) {
            int off = offsets[idx];
            int len = lengths[idx];
            byte nxt = nextBytes[idx];

            if (off == 0 && len == 0) {
                // literal
                if (outPos + 1 > out.length) out = Arrays.copyOf(out, out.length * 2 + 1);
                out[outPos++] = nxt;
            } else {
                int start = outPos - off;
                int need = outPos + len + 1;
                if (need > out.length) out = Arrays.copyOf(out, Math.max(need, out.length * 2));
                for (int j = 0; j < len; j++) {
                    out[outPos++] = out[start + j];
                }
                boolean lastTripleAndZeroNext = (idx == count - 1 && nxt == 0);
                if (!lastTripleAndZeroNext) {
                    if (outPos + 1 > out.length) out = Arrays.copyOf(out, out.length * 2 + 1);
                    out[outPos++] = nxt;
                }
            }
        }
        return Arrays.copyOf(out, outPos);
    }
}
