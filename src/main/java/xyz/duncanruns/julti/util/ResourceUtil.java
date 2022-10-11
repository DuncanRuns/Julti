package xyz.duncanruns.julti.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class ResourceUtil {

    public static BufferedImage getImageResource(String name) throws IOException {
        return ImageIO.read(getResource(name));
    }

    public static URL getResource(String name) {
        return ResourceUtil.class.getResource(name);
    }
}
