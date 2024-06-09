package xyz.duncanruns.julti.util;

import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.cancelrequester.CancelRequesters;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.win32.User32;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static xyz.duncanruns.julti.Julti.log;
import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public final class SafeInstanceLauncher {
    // Safely launches instance(s) so that MultiMC does not crash.
    private static final Set<String> executableNames = ImmutableSet.<String>builder().add("multimc.exe", "prismlauncher.exe").build();

    private SafeInstanceLauncher() {
    }

    public static void launchInstance(MinecraftInstance instance) {
        launchInstance(instance, CancelRequesters.NEVER_CANCEL_REQUESTER);
    }

    public static void launchInstance(MinecraftInstance instance, CancelRequester cancelRequester) {
        JultiOptions options = JultiOptions.getJultiOptions();
        String multiMCPath = options.multiMCPath;
        if (Files.isDirectory(Paths.get(multiMCPath))) {
            Optional<Path> actualExe = Optional.empty();
            try {
                actualExe = Files.list(Paths.get(multiMCPath)).filter(p -> executableNames.stream().anyMatch(p.getFileName().toString()::equalsIgnoreCase)).findAny();
            } catch (IOException ignored) {
            }
            if (actualExe.isPresent()) {
                Julti.log(Level.WARN, "You selected a MultiMC folder instead of the exe. I've fixed that for you.");
                multiMCPath = actualExe.get().toAbsolutePath().toString();
                options.multiMCPath = multiMCPath;
            } else {
                log(Level.ERROR, "Could not launch " + instance + " (invalid MultiMC.exe path).");
                return;
            }
        }
        if (instance.hasWindow()) {
            log(Level.ERROR, "Could not launch " + instance + " (already open).");
            return;
        }
        if (multiMCPath.isEmpty() || !Files.exists(Paths.get(multiMCPath))) {
            log(Level.ERROR, "Could not launch " + instance + " (invalid MultiMC.exe path).");
            return;
        }
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        new Thread(() -> launchInstanceInternal(instance, cancelRequester), "instance-launcher").start();
    }

    private static void launchInstanceInternal(MinecraftInstance instance, CancelRequester cancelRequester) {
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        log(Level.INFO, "Launching instance...");
        JultiOptions options = JultiOptions.getJultiOptions();
        String multiMCPath = options.multiMCPath;
        try {
            if (!startMultiMC(multiMCPath, cancelRequester)) {
                log(Level.ERROR, "MultiMC did not start! Try ending it in task manager and opening it manually.");
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        boolean launchOffline = options.launchOffline;
        Path multiMCActualPath = Paths.get(multiMCPath);
        if (launchOffline && multiMCActualPath.getName(multiMCActualPath.getNameCount() - 1).toString().contains("prism")) {
            launchOffline = false;
            log(Level.WARN, "Warning: Prism Launcher cannot use offline launching!");
        }
        sleep(200);
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        int instanceNum = InstanceManager.getInstanceManager().getInstanceNum(instance);
        instance.launch(launchOffline ? (options.launchOfflineName.replace("*", String.valueOf(instanceNum))) : null);
    }

    private static boolean startMultiMC(String multiMCLocation, CancelRequester cancelRequester) throws IOException {
        String[] multiMCPathArgs = multiMCLocation.replace('\\', '/').split("/");
        String exeName = multiMCPathArgs[multiMCPathArgs.length - 1];
        if (multiMCExists(exeName)) {
            return true;
        }
        LauncherUtil.openFile(multiMCLocation);
        int tries = 0;
        while ((!multiMCExists(exeName)) && (!cancelRequester.isCancelRequested())) {
            if (++tries > 50) {
                return false;
            }
            sleep(200);
        }
        return true;
    }

    private static boolean multiMCExists(String exeName) {
        AtomicBoolean out = new AtomicBoolean(false);
        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            if (PidUtil.getProcessExecutable(PidUtil.getPidFromHwnd(hWnd)).endsWith(exeName)) {
                out.set(true);
                return false;
            }
            return true;
        }, null);
        return out.get();
    }

    public static void launchInstances(List<MinecraftInstance> instances) {
        launchInstances(instances, CancelRequesters.NEVER_CANCEL_REQUESTER);
    }

    public static void launchInstances(List<MinecraftInstance> instances, CancelRequester cancelRequester) {
        String multiMCPath = JultiOptions.getJultiOptions().multiMCPath;
        if (multiMCPath.isEmpty() || !Files.exists(Paths.get(multiMCPath))) {
            log(Level.ERROR, "Could not launch instances (invalid MultiMC.exe path).");
            return;
        }
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        new Thread(() -> launchInstancesInternal(instances, cancelRequester), "instance-launcher").start();
    }

    private static void launchInstancesInternal(List<MinecraftInstance> instances, CancelRequester cancelRequester) {
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        log(Level.INFO, "Launching instances...");
        JultiOptions options = JultiOptions.getJultiOptions();
        String multiMCPath = options.multiMCPath;
        try {
            if (!startMultiMC(multiMCPath, cancelRequester)) {
                log(Level.ERROR, "MultiMC did not start! Try ending it in task manager and opening it manually.");
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        boolean launchOffline = options.launchOffline;
        Path multiMCActualPath = Paths.get(multiMCPath);
        if (launchOffline && multiMCActualPath.getName(multiMCActualPath.getNameCount() - 1).toString().contains("prism")) {
            launchOffline = false;
            log(Level.WARN, "Warning: Prism Launcher cannot use offline launching!");
        }

        sleep(200);
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        for (MinecraftInstance instance : instances) {
            int instanceNum = instances.indexOf(instance) + 1;
            if (instance.hasWindow()) {
                continue;
            }
            sleep(JultiOptions.getJultiOptions().launchDelay);
            if (cancelRequester.isCancelRequested()) {
                return;
            }
            instance.launch(launchOffline ? (options.launchOfflineName.replace("*", String.valueOf(instanceNum))) : null);
        }
    }
}
