package xyz.duncanruns.julti;

import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;

public final class ResetCounter {
    private static final Object LOCK = new Object();

    public static void increment() {
        synchronized (LOCK) {
            JultiOptions.getInstance().resetCounter += 1;
        }
        updateFile();
    }

    public static void updateFile() {
        new Thread(() -> {
            try {
                synchronized (LOCK) {
                    FileUtil.writeString(JultiOptions.getJultiDir().resolve("resets.txt"), String.valueOf(JultiOptions.getInstance().resetCounter));
                }
            } catch (IOException ignored) {}
        }, "reset-file-updater").start();
    }
}
