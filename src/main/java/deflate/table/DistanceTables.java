package deflate.table;

public final class DistanceTables {
    public static final int[][] CODE_EQUAL_BASE_CODE_EXTRABIT = {
        {1, 0, 0},
        {2, 1, 0},
        {3, 2, 0},
        {4, 3, 0},
        {5, 4, 1},
        {7, 5, 1},
        {9, 6, 2},
        {13, 7, 2},
        {17, 8, 3},
        {25, 9, 3},
        {33, 10, 4},
        {49, 11, 4},
        {65, 12, 5},
        {97, 13, 5},
        {129, 14, 6},
        {193, 15, 6},
        {257, 16, 7},
        {385, 17, 7},
        {513, 18, 8},
        {769, 19, 8},
        {1025, 20, 9},
        {1537, 21, 9},
        {2049, 22, 10},
        {3073, 23, 10},
        {4097, 24, 11},
        {6145, 25, 11},
        {8193, 26, 12},
        {12289, 27, 12},
        {16385, 28, 13},
        {24577, 29, 13},
        //dummy
        {32769, 0, 0}
    };

    private static final int[] LOOKUP_TABLE = new int[32768 + 1];

    static {
        int index = 0;
        for (int distance = 1; distance <= 32768; distance++) {
            if(CODE_EQUAL_BASE_CODE_EXTRABIT[index + 1][0] == distance) {
                index++;
            }
            LOOKUP_TABLE[distance] = index;
        }
    }


    public static int[] search(int distance) {
        return CODE_EQUAL_BASE_CODE_EXTRABIT[LOOKUP_TABLE[distance]];
    }
}
