package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class ResetCounter {
    private final static Object LOCK = new Object();
    private final static List<Supplier<Boolean>> LOCK_CONDITIONS = new CopyOnWriteArrayList<>();
    public static int sessionCounter = 0;

    private ResetCounter() {
    }

    public static void registerLockCondition(Supplier<Boolean> lockCondition) {
        LOCK_CONDITIONS.add(lockCondition);
    }

    public static void increment() {
        if (LOCK_CONDITIONS.stream().anyMatch(Supplier::get)) {
            return;
        }
        new Thread(ResetCounter::incrementInternal, "reset-counter-updater").start();
    }

    public static void updateFiles() {
        new Thread(() -> {
            synchronized (LOCK) {
                updateFileInternal(JultiOptions.getJultiOptions().resetCounter, JultiOptions.getJultiDir().resolve("resets.txt"));
                updateFileInternal(sessionCounter, JultiOptions.getJultiDir().resolve("sessionresets.txt"));
            }
        }, "reset-counter-updater").start();
    }

    private static void incrementInternal() {
        synchronized (LOCK) {
            JultiOptions options = JultiOptions.getJultiOptions();
            updateFileInternal(++options.resetCounter, JultiOptions.getJultiDir().resolve("resets.txt"));

            if (options.resetCounter % 100 == 0) {
                Julti.doLater(options::trySave);
            }

            updateFileInternal(++sessionCounter, JultiOptions.getJultiDir().resolve("sessionresets.txt"));
        }
    }

    private static void updateFileInternal(int count, Path path) {
        try {
            FileUtil.writeString(path, String.valueOf(count));
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to write resets.txt:\n" + ExceptionUtil.toDetailedString(e));
        }
    }
}
