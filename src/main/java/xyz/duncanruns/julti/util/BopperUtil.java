package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class BopperUtil {
    private static boolean clearing = false;

    private BopperUtil() {
    }

    public synchronized static void clearWorlds() {
        if (clearing) {
            return;
        }
        clearing = true;
        new Thread(BopperUtil::clearWorldsInternal, "bopper-util").start();
    }

    private static void clearWorldsInternal() {
        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
        Thread[] threads = new Thread[instances.size()];
        int i = 0;
        for (MinecraftInstance instance : instances) {
            final MinecraftInstance instanceI = instance;
            threads[i] = new Thread(() -> tryClearWorlds(instanceI));
            threads[i].start();
            i++;
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
            Julti.log(Level.INFO, "Finished clearing worlds!");
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Failed to clear worlds: " + e);
        } finally {
            clearing = false;
        }
    }

    private static void tryClearWorlds(MinecraftInstance instance) {
        try {
            clearWorlds(instance);
        } catch (Exception e) {
            Julti.log(Level.ERROR, "Clear Worlds Exception:\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void clearWorlds(MinecraftInstance instance) throws IOException {
        Path savesPath = instance.getPath().resolve("saves");
        // Check if saves folder exists first
        if (!Files.isDirectory(savesPath)) {
            return;
        }
        // Get all worlds that are allowed to be deleted
        List<Path> worldsToRemove = Arrays.stream(Objects.requireNonNull(savesPath.toFile().list())) // Get all world names
                .map(savesPath::resolve) // Map to world paths
                .filter(BopperUtil::shouldDelete) // Filter for only ones that should be deleted
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
        // Remove the first 6 (or less) worlds
        worldsToRemove.subList(0, Math.min(6, worldsToRemove.size())).clear();
        // Actually delete stuff
        int i = 0;
        int total = worldsToRemove.size();
        for (Path path : worldsToRemove) {
            if (++i % 50 == 0) {
                Julti.log(Level.INFO, "Clearing " + instance.getName() + ": " + i + "/" + total);
            }
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private static boolean shouldDelete(Path path) {
        if ((!path.toFile().isDirectory()) || (path.resolve("Reset Safe.txt").toFile().isFile()) || !Files.isRegularFile(path.resolve("level.dat"))) {
            return false;
        }
        String name = path.getFileName().toString();
        if (name.startsWith("_")) {
            return false;
        }
        return name.startsWith("New World") || name.contains("Speedrun #") || name.contains("Practice Seed") || name.contains("Seed Paster");
    }
}
