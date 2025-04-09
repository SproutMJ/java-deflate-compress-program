package deflate.header;

import java.util.List;
import java.util.Map;

public class Header {
    public static final int[] CODE_LENGTH_CODE_ORDER = {
            16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
    };

    private long bfinal;
    private long btype;
    private int hlit;
    private int hdist;
    private int hclen;


    private int[] codeLengthCodeLengths;
    private List<Integer> rleEncodedLengths;
    private Map<Integer, Long> codeLengthCodes;


    private Map<Integer, Integer> codeLengthCodeLengthsMap;
    private Map<Long, Integer> codeLengthAlphabetTree;
    private List<Integer> decompressedCodeLengths;
    private Map<Integer, Integer> literalLengths;
    private Map<Integer, Integer> distanceLengths;
    private Map<Long, Integer> literalTree;
    private Map<Long, Integer> distanceTree;

    private Header() {
    }

    private Header(long bfinal, long btype) {
        this.bfinal = bfinal;
        this.btype = btype;
    }

    private Header(long bfinal, long btype, int hlit, int hdist, int hclen, int[] codeLengthCodeLengths, List<Integer> rleEncodedLengths, Map<Integer, Long> codeLengthCodes) {
        this.bfinal = bfinal;
        this.btype = btype;
        this.hlit = hlit;
        this.hdist = hdist;
        this.hclen = hclen;
        this.codeLengthCodeLengths = codeLengthCodeLengths;
        this.rleEncodedLengths = rleEncodedLengths;
        this.codeLengthCodes = codeLengthCodes;
    }

    private Header(long bfinal, long btype, int hlit, int hdist, int hclen, Map<Integer, Integer> codeLengthCodeLengthsMap, Map<Long, Integer> codeLengthAlphabetTree, List<Integer> decompressedCodeLengths, Map<Integer, Integer> literalLengths, Map<Integer, Integer> distanceLengths, Map<Long, Integer> literalTree, Map<Long, Integer> distanceTree) {
        this.bfinal = bfinal;
        this.btype = btype;
        this.hlit = hlit;
        this.hdist = hdist;
        this.hclen = hclen;
        this.codeLengthCodeLengthsMap = codeLengthCodeLengthsMap;
        this.codeLengthAlphabetTree = codeLengthAlphabetTree;
        this.decompressedCodeLengths = decompressedCodeLengths;
        this.literalLengths = literalLengths;
        this.distanceLengths = distanceLengths;
        this.literalTree = literalTree;
        this.distanceTree = distanceTree;
    }

    public static Header createEncodedHeader(long bfinal, long btype, int hlit, int hdist, int hclen, int[] codeLengthCodeLengths, List<Integer> rleEncodedLengths, Map<Integer, Long> codeLengthCodes) {
        return new Header(bfinal, btype, hlit, hdist, hclen, codeLengthCodeLengths, rleEncodedLengths, codeLengthCodes);
    }

    public static Header createDecodedHeaderDynamicCompressed(long bfinal, long btype, int hlit, int hdist, int hclen, Map<Integer, Integer> codeLengthCodeLengthsMap, Map<Long, Integer> codeLengthAlphabetTree, List<Integer> decompressedCodeLengths, Map<Integer, Integer> literalLengths, Map<Integer, Integer> distanceLengths, Map<Long, Integer> literalTree, Map<Long, Integer> distanceTree) {
        return new Header(bfinal, btype, hlit, hdist, hclen, codeLengthCodeLengthsMap, codeLengthAlphabetTree, decompressedCodeLengths, literalLengths, distanceLengths, literalTree, distanceTree);
    }

    public static Header createDecodedHeaderNoneCompressed(long bfinal, long btype) {
        return new Header(bfinal, btype);
    }

    public long getBfinal() {
        return bfinal;
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

    public Map<Integer, Integer> getCodeLengthCodeLengthsMap() {
        return codeLengthCodeLengthsMap;
    }

    public Map<Long, Integer> getCodeLengthAlphabetTree() {
        return codeLengthAlphabetTree;
    }

    public List<Integer> getDecompressedCodeLengths() {
        return decompressedCodeLengths;
    }

    public Map<Integer, Integer> getLiteralLengths() {
        return literalLengths;
    }

    public Map<Integer, Integer> getDistanceLengths() {
        return distanceLengths;
    }

    public Map<Long, Integer> getLiteralTree() {
        return literalTree;
    }

    public Map<Long, Integer> getDistanceTree() {
        return distanceTree;
    }
}
