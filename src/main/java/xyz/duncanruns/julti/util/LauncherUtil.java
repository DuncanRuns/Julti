package xyz.duncanruns.julti.util;

import com.sun.jna.platform.win32.Shell32;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.JultiOptions;

import java.awt.*;
import java.nio.file.Paths;

import static xyz.duncanruns.julti.Julti.log;

public class LauncherUtil {
    private LauncherUtil() {
    }

    public static void launchPrograms() {
        JultiOptions options = JultiOptions.getJultiOptions();

        if (!Desktop.isDesktopSupported()) {
            log(Level.ERROR, "Could not launch programs: Java Desktop not supported.");
            return;
        }

        options.launchingProgramPaths.forEach(LauncherUtil::openFile);
    }

    public static void openFile(String path) {
        Shell32.INSTANCE.ShellExecute(null, "open", path, null, Paths.get(path).getParent().toString(), 1);
    }
}
