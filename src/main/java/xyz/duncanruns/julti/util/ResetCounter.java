package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;

public final class ResetCounter {
    private final static Object LOCK = new Object();

    private ResetCounter() {
    }

    public static void increment() {
        new Thread(ResetCounter::incrementInternal, "reset-counter-updater").start();
    }

    private static void incrementInternal() {
        synchronized (LOCK) {
            updateFile(JultiOptions.getInstance().resetCounter++);
        }
    }

    private static void updateFile(int count) {
        try {
            FileUtil.writeString(JultiOptions.getJultiDir().resolve("resets.txt"), String.valueOf(count));
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to write resets.txt:\n" + ExceptionUtil.toDetailedString(e));
        }
    }
}
