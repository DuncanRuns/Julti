package xyz.duncanruns.julti.util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
    public static void writeString(Path path, String string) throws IOException {
        FileWriter writer = new FileWriter(path.toFile());
        writer.write(string);
        writer.close();
    }

    public static String readString(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }
}
