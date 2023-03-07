package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class SafeInstanceLauncher {
    // Safely launches instance(s) so that MultiMC does not crash.
    private SafeInstanceLauncher() {
    }

    public static boolean launchInstance(MinecraftInstance instance, Julti julti) {
        if (instance.hasWindow()) return false;
        JultiOptions options = JultiOptions.getInstance();
        String multiMCPath = options.multiMCPath;
        if (multiMCPath.isEmpty()) {
            return false;
        }
        try {
            startMultiMC(multiMCPath);
        } catch (IOException e) {
            return false;
        }
        boolean launchOffline = options.launchOffline;
        Path multiMCActualPath = Paths.get(multiMCPath);
        if (launchOffline && multiMCActualPath.getName(multiMCActualPath.getNameCount() - 1).toString().contains("prism")) {
            launchOffline = false;
            JultiGUI.log(Level.WARN, "Warning: Prism Launcher cannot use offline names!");
        }
        boolean finalLaunchOffline = launchOffline;
        new Timer("delayed-launcher").schedule(new TimerTask() {
            @Override
            public void run() {

                int instanceNum = julti.getInstanceManager().getInstances().indexOf(instance) + 1;
                instance.launch(finalLaunchOffline ? (options.launchOfflinePrefix + "_" + instanceNum) : null);
            }
        }, 1000);
        return true;
    }

    private static void startMultiMC(String multiMCLocation) throws IOException {
        if (HwndUtil.multiMCExists()) {
            return;
        }
        Runtime.getRuntime().exec(multiMCLocation);
        while (!HwndUtil.multiMCExists()) {
            sleep(200);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean launchInstances(List<MinecraftInstance> instances) {
        JultiOptions options = JultiOptions.getInstance();
        String multiMCPath = options.multiMCPath;
        if (multiMCPath.isEmpty() || !Files.exists(Paths.get(multiMCPath))) {
            return false;
        }
        try {
            startMultiMC(multiMCPath);
        } catch (Exception e) {
            return false;
        }
        new Timer("delayed-launcher").schedule(new TimerTask() {
            @Override
            public void run() {
                for (MinecraftInstance instance : instances) {
                    int instanceNum = instances.indexOf(instance) + 1;
                    if (instance.hasWindow()) continue;
                    sleep(500);
                    instance.launch(options.launchOffline ? (options.launchOfflinePrefix + "_" + instanceNum) : null);
                }
            }
        }, 1000);
        return true;
    }
}
