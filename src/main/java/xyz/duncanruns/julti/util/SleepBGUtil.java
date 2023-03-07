package xyz.duncanruns.julti.util;

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
            if (!Files.exists(SLEEP_LOCK_PATH))
                FileUtil.writeString(SLEEP_LOCK_PATH, "");
        } catch (IOException ignored) {
        }
    }

    public static void disableLock() {
        try {
            Files.deleteIfExists(SLEEP_LOCK_PATH);
        } catch (IOException ignored) {
        }
    }
}
