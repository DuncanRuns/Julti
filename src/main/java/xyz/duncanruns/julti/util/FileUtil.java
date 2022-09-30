package xyz.duncanruns.julti.util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

public final class FileUtil {
    // Replacement for Files for java 8 compatibility

    private FileUtil() {
    }

    public static void writeString(Path path, CharSequence charSequence) throws IOException {
        FileWriter fileWriter = new FileWriter(path.toFile());
        fileWriter.write(charSequence.toString());
        fileWriter.close();
    }

    public static String readString(Path path) throws IOException {
        Scanner scanner = new Scanner(path);
        StringBuilder out = new StringBuilder();
        while (scanner.hasNextLine()) {
            out.append(scanner.nextLine());
        }
        scanner.close();
        return out.toString();
    }
}
