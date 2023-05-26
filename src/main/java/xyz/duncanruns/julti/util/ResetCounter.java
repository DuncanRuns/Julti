package xyz.duncanruns.julti.util;

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
        } catch (Exception ignored) {
        }
    }
}
