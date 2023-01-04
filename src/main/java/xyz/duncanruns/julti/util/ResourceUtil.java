package xyz.duncanruns.julti.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;

public final class ResourceUtil {
    private ResourceUtil() {

    }

    public static BufferedImage getImageResource(String name) throws IOException {
        return ImageIO.read(getResource(name));
    }

    public static URL getResource(String name) {
        return ResourceUtil.class.getResource(name);
    }

    public static void copyResourceToFile(String resourceName, Path destination) throws IOException {
        // Answer to https://stackoverflow.com/questions/10308221/how-to-copy-file-inside-jar-to-outside-the-jar
        InputStream inStream = getResourceAsStream(resourceName);
        OutputStream outStream = new FileOutputStream(destination.toFile());
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
