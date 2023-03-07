package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class SafeInstanceLauncher {
    // Safely launches instance(s) so that MultiMC does not crash.
    private static final Logger LOGGER = LogManager.getLogger("InstanceLauncher");

    private SafeInstanceLauncher() {
    }

    public static void launchInstance(MinecraftInstance instance, Julti julti) {
        String multiMCPath = JultiOptions.getInstance().multiMCPath;
        if (instance.hasWindow()) {
            log(Level.ERROR, "Could not launch " + instance + " (already open).");
            return;
        }
        if (multiMCPath.isEmpty() || !Files.exists(Paths.get(multiMCPath))) {
            log(Level.ERROR, "Could not launch " + instance + " (invalid MultiMC.exe path).");
            return;
        }
        new Thread(() -> launchInstanceInternal(instance, julti), "instance-launcher").start();
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    private static void launchInstanceInternal(MinecraftInstance instance, Julti julti) {
        log(Level.INFO, "Launching instance...");
        JultiOptions options = JultiOptions.getInstance();
        String multiMCPath = options.multiMCPath;
        try {
            if (!startMultiMC(multiMCPath)) {
                log(Level.ERROR, "MultiMC did not start! Try ending it in task manager and opening it manually.");
                return;
            }
        } catch (IOException e) {
            return;
        }
        boolean launchOffline = options.launchOffline;
        Path multiMCActualPath = Paths.get(multiMCPath);
        if (launchOffline && multiMCActualPath.getName(multiMCActualPath.getNameCount() - 1).toString().contains("prism")) {
            launchOffline = false;
            JultiGUI.log(Level.WARN, "Warning: Prism Launcher cannot use offline names!");
        }
        sleep(200);
        int instanceNum = julti.getInstanceManager().getInstances().indexOf(instance) + 1;
        instance.launch(launchOffline ? (options.launchOfflinePrefix + "_" + instanceNum) : null);
    }

    private static boolean startMultiMC(String multiMCLocation) throws IOException {
        String[] multiMCPathArgs = multiMCLocation.replace('\\', '/').split("/");
        String exeName = multiMCPathArgs[multiMCPathArgs.length - 1];
        if (HwndUtil.multiMCExists(exeName)) {
            return true;
        }
        Runtime.getRuntime().exec(multiMCLocation);
        int tries = 0;
        while (!HwndUtil.multiMCExists(exeName)) {
            if (++tries > 50) {
                return false;
            }
            sleep(200);
        }
        return true;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void launchInstances(List<MinecraftInstance> instances) {
        String multiMCPath = JultiOptions.getInstance().multiMCPath;
        if (multiMCPath.isEmpty() || !Files.exists(Paths.get(multiMCPath))) {
            log(Level.ERROR, "Could not launch instances (invalid MultiMC.exe path).");
            return;
        }
        new Thread(() -> launchInstancesInternal(instances), "instance-launcher").start();
    }

    private static void launchInstancesInternal(List<MinecraftInstance> instances) {
        log(Level.INFO, "Launching instances...");
        JultiOptions options = JultiOptions.getInstance();
        String multiMCPath = options.multiMCPath;
        try {
            if (!startMultiMC(multiMCPath)) {
                log(Level.ERROR, "MultiMC did not start! Try ending it in task manager and opening it manually.");
                return;
            }
        } catch (Exception e) {
            return;
        }
        sleep(200);
        for (MinecraftInstance instance : instances) {
            int instanceNum = instances.indexOf(instance) + 1;
            if (instance.hasWindow()) continue;
            sleep(500);
            instance.launch(options.launchOffline ? (options.launchOfflinePrefix + "_" + instanceNum) : null);
        }
    }
}
