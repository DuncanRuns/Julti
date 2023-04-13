package xyz.duncanruns.julti.util;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;

public final class ResetCounter {
    private final static Object WRITE_LOCK = new Object();

    private ResetCounter() {
    }

    public static void increment() {
        Julti.doLater(() -> {
            int i = JultiOptions.getInstance().resetCounter++;
            new Thread(() -> updateFile(i), "reset-counter-updater").start();
        });
    }

    private static void updateFile(int count) {
        synchronized (WRITE_LOCK) {
            try {
                FileUtil.writeString(JultiOptions.getJultiDir().resolve("resets.txt"), String.valueOf(JultiOptions.getInstance().resetCounter));
            } catch (Exception ignored) {
            }
        }
    }
}
