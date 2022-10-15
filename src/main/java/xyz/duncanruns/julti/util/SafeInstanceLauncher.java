package xyz.duncanruns.julti.util;

import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class SafeInstanceLauncher {
    // Safely launches instance(s) so that multimc/polymc does not crash.
    private SafeInstanceLauncher() {
    }

    public static boolean launchInstance(MinecraftInstance instance) {
        String multiMCPath = JultiOptions.getInstance().multiMCPath;
        if (multiMCPath.isEmpty()) {
            return false;
        }
        try {
            Runtime.getRuntime().exec(multiMCPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Timer("delayed-launcher").schedule(new TimerTask() {
            @Override
            public void run() {
                instance.launch();
            }
        }, 1000);
        return true;
    }

    public static boolean launchInstances(List<MinecraftInstance> instances) {

        String multiMCPath = JultiOptions.getInstance().multiMCPath;
        if (multiMCPath.isEmpty()) {
            return false;
        }
        try {
            Runtime.getRuntime().exec(multiMCPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Timer("delayed-launcher").schedule(new TimerTask() {
            @Override
            public void run() {
                for (MinecraftInstance instance : instances) {
                    sleep(500);
                    instance.launch();
                }
            }
        }, 500);
        return true;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
