package xyz.duncanruns.julti.util;

public final class FabricCrashUtil {
    private FabricCrashUtil() {
    }

    public void onInitialize() {
        throw new RuntimeException("Julti is not supposed to be ran as a mod!");
    }
}
