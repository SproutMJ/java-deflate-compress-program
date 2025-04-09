package launcher;

import deflate.Deflate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static Scanner scanner = new Scanner(System.in);

    public static List<String> filePath() {
        scanner.nextLine();
        System.out.println("압축 파일 경로 입력");
        String dest = scanner.nextLine();
        System.out.println("해제 파일 경로 입력");
        String source = scanner.nextLine();

        return new ArrayList<>(Arrays.asList(dest, source));
    }

    public static void main(String[] args) {
        try {
            Deflate deflate = new Deflate();
            deflate.compress("", "");

            deflate.decompress("", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
