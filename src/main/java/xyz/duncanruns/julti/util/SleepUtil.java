package xyz.duncanruns.julti.util;

public final class SleepUtil {
    private SleepUtil() {
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
