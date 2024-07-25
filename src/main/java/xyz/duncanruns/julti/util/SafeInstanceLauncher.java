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
    private static final Set<String> multiMCExecutableNames = ImmutableSet.<String>builder().add("multimc.exe", "prismlauncher.exe").build();
    private static final Set<String> colorMCExecutableNames = ImmutableSet.<String>builder().add("ColorMC.Launcher.exe").build();

    private SafeInstanceLauncher() {
    }

    public static void launchInstance(MinecraftInstance instance) {
        launchInstance(instance, CancelRequesters.NEVER_CANCEL_REQUESTER);
    }

    public static void launchInstance(MinecraftInstance instance, CancelRequester cancelRequester) {
        if (instance.hasWindow()) {
            log(Level.ERROR, "Could not launch " + instance + " (already open).");
            return;
        }
        if (instance.getInstanceType() == MinecraftInstance.InstanceType.MultiMC &&
                isInvalidLauncherPath(getLauncherPath(MinecraftInstance.InstanceType.MultiMC))) {
            log(Level.ERROR, "Could not launch " + instance + " (invalid MultiMC.exe path).");
            return;
        }
        if (instance.getInstanceType() == MinecraftInstance.InstanceType.ColorMC &&
                isInvalidLauncherPath(getLauncherPath(MinecraftInstance.InstanceType.ColorMC))) {
            log(Level.ERROR, "Could not launch " + instance + " (invalid ColorMC.Launcher.exe path).");
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
        boolean launchOffline = options.launchOffline;
        if (instance.getInstanceType() == MinecraftInstance.InstanceType.MultiMC) {
            String multiMCPath = getLauncherPath(MinecraftInstance.InstanceType.MultiMC);
            try {
                if (!startLauncher(multiMCPath, cancelRequester)) {
                    log(Level.ERROR, "MultiMC did not start! Try ending it in task manager and opening it manually.");
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (cancelRequester.isCancelRequested()) {
                return;
            }
            Path multiMCActualPath = Paths.get(multiMCPath);
            if (launchOffline && multiMCActualPath.getName(multiMCActualPath.getNameCount() - 1).toString().contains("prism")) {
                launchOffline = false;
                log(Level.WARN, "Warning: Prism Launcher cannot use offline launching!");
            }
        }

        sleep(200);
        if (cancelRequester.isCancelRequested()) {
            return;
        }
        int instanceNum = InstanceManager.getInstanceManager().getInstanceNum(instance);
        instance.launch(launchOffline ? (options.launchOfflineName.replace("*", String.valueOf(instanceNum))) : null);
    }

    private static boolean startLauncher(String launcherLocation, CancelRequester cancelRequester) throws IOException {
        String[] launcherPathArgs = launcherLocation.replace('\\', '/').split("/");
        String exeName = launcherPathArgs[launcherPathArgs.length - 1];
        if (launcherExists(exeName)) {
            return true;
        }
        LauncherUtil.openFile(launcherLocation);
        int tries = 0;
        while ((!launcherExists(exeName)) && (!cancelRequester.isCancelRequested())) {
            if (++tries > 50) {
                return false;
            }
            sleep(200);
        }
        return true;
    }

    private static boolean launcherExists(String exeName) {
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

        boolean usesMMC = instances.stream().map(MinecraftInstance::getInstanceType).anyMatch(MinecraftInstance.InstanceType.MultiMC::equals);
        boolean usesCMC = instances.stream().map(MinecraftInstance::getInstanceType).anyMatch(MinecraftInstance.InstanceType.ColorMC::equals);

        boolean mmcok = false;
        String multiMCPath = getLauncherPath(MinecraftInstance.InstanceType.MultiMC);
        if (usesMMC) {
            try {
                if (isInvalidLauncherPath(multiMCPath)) {
                    log(Level.ERROR, "Could not launch instances (invalid MultiMC.exe path).");
                } else {
                    if (!startLauncher(multiMCPath, cancelRequester)) {
                        log(Level.ERROR, "MultiMC did not start! Try ending it in task manager and opening it manually.");
                    } else {
                        mmcok = true;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (usesCMC) {
            String colorMCPath = getLauncherPath(MinecraftInstance.InstanceType.ColorMC);
            try {
                if (isInvalidLauncherPath(colorMCPath)) {
                    log(Level.ERROR, "Could not launch instances (invalid ColorMC.Launcher.exe path).");
                } else if (!startLauncher(colorMCPath, cancelRequester)) {
                    log(Level.ERROR, "ColorMC did not start! Try ending it in task manager and opening it manually.");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (cancelRequester.isCancelRequested()) {
            return;
        }
        JultiOptions options = JultiOptions.getJultiOptions();
        boolean launchOffline = options.launchOffline;
        if (mmcok) {
            Path multiMCActualPath = Paths.get(multiMCPath);
            if (launchOffline && multiMCActualPath.getName(multiMCActualPath.getNameCount() - 1).toString().contains("prism")) {
                launchOffline = false;
                log(Level.WARN, "Warning: Prism Launcher cannot use offline launching!");
            }
        }

        if (launchOffline && usesCMC) {
            log(Level.WARN, "Warning: ColorMC cannot use offline launching!");
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

    private static boolean isInvalidLauncherPath(String launcherPath) {
        return launcherPath.isEmpty() || !Files.exists(Paths.get(launcherPath)) || !launcherPath.endsWith(".exe");
    }

    private static String getLauncherPath(MinecraftInstance.InstanceType type) {
        JultiOptions options = JultiOptions.getJultiOptions();
        String path = type == MinecraftInstance.InstanceType.ColorMC ? options.colorMCPath : options.multiMCPath;
        Path originalPath = Paths.get(path);
        if (Files.isDirectory(originalPath)) {
            Optional<Path> actualExe = Optional.empty();
            try {
                if (type == MinecraftInstance.InstanceType.MultiMC) {
                    actualExe = Files.list(originalPath).filter(p -> multiMCExecutableNames.stream().anyMatch(p.getFileName().toString()::equalsIgnoreCase)).findAny();
                } else if (type == MinecraftInstance.InstanceType.ColorMC) {
                    actualExe = Files.list(originalPath).filter(p -> colorMCExecutableNames.stream().anyMatch(p.getFileName().toString()::equalsIgnoreCase)).findAny();
                }
            } catch (IOException ignored) {
            }
            if (actualExe.isPresent()) {
                Julti.log(Level.WARN, "You selected a Launcher folder instead of the exe. I've fixed that for you.");
                path = actualExe.get().toAbsolutePath().toString();
                if (type == MinecraftInstance.InstanceType.MultiMC) {
                    options.multiMCPath = path;
                } else {
                    options.colorMCPath = path;
                }
            }
        }
        return path;
    }
}
