package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SleepBGUtil {
    private static final Path SLEEP_LOCK_PATH = Paths.get(System.getProperty("user.home")).resolve("sleepbg.lock");

    private SleepBGUtil() {
    }

    public static void enableLock() {
        try {
            if (!Files.exists(SLEEP_LOCK_PATH)) {
                FileUtil.writeString(SLEEP_LOCK_PATH, "");
            }
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to enable sleepbg.lock:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    public static void disableLock() {
        try {
            Files.deleteIfExists(SLEEP_LOCK_PATH);
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Failed to disable sleepbg.lock:\n" + ExceptionUtil.toDetailedString(e));
        }
    }
}