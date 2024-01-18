package xyz.duncanruns.julti.util;

import static xyz.duncanruns.julti.Julti.log;

import java.awt.Desktop;
import java.io.IOException;

import org.apache.logging.log4j.Level;

import xyz.duncanruns.julti.JultiOptions;

public class LauncherUtil {
    private LauncherUtil() {
    }

    public static void launchPrograms() {
        JultiOptions options = JultiOptions.getJultiOptions();

        if (!Desktop.isDesktopSupported()) {
            log(Level.ERROR, "Could not launch programs: Java Desktop not supported.");
            return;
        }

        options.launchingProgramPaths.forEach(path -> {
            try {
                // FIXME: This fails with programs like OBS because it opens them from directory julti is being ran, not their parent directory
                Desktop.getDesktop().open(path.toFile());
            } catch (IOException e) {
                log(Level.ERROR, "Could not launch program \"" + path.toString() + "\": " + ExceptionUtil.toDetailedString(e));
            }
        });
    }
}
