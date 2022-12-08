package xyz.duncanruns.julti;

import java.io.IOException;
import java.nio.file.Files;

public final class ResetCounter {
    private static final Object LOCK = new Object();

    private ResetCounter() {
    }

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
                    Files.writeString(JultiOptions.getJultiDir().resolve("resets.txt"), String.valueOf(JultiOptions.getInstance().resetCounter));
                }
            } catch (IOException ignored) {
            }
        }).start();
    }

    public static void set(int amount) {
        synchronized (LOCK) {
            JultiOptions.getInstance().resetCounter = amount;
        }
        updateFile();
    }
}
