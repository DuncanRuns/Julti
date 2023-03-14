package xyz.duncanruns.julti.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ResourceUtil {
    /**
     * <a href="https://stackoverflow.com/questions/10308221/how-to-copy-file-inside-jar-to-outside-the-jar">Source</a>
     */
    public static void copyResourceToFile(String resourceName, Path destination) throws IOException {
        InputStream inStream = getResourceAsStream(resourceName);
        OutputStream outStream = Files.newOutputStream(destination.toFile().toPath());
        int readBytes;
        byte[] buffer = new byte[4096];
        while ((readBytes = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, readBytes);
        }
        inStream.close();
        outStream.close();
    }

    public static InputStream getResourceAsStream(String name) {
        return ResourceUtil.class.getResourceAsStream(name);
    }
}
