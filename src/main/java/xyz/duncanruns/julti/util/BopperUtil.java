package xyz.duncanruns.julti.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        List<Path> worldsToRemove = new ArrayList<>();
        for (String string : savesPath.toFile().list()) {
            if (!string.startsWith("_")) {
                worldsToRemove.add(savesPath.resolve(string));
            }
        }
        worldsToRemove.removeIf(path -> (!path.toFile().isDirectory()) || (path.resolve("Reset Safe.txt").toFile().isFile()));
        worldsToRemove.sort((o1, o2) -> (int) (o2.toFile().lastModified() - o1.toFile().lastModified()));
        for (int i = 0; i < 6 && !worldsToRemove.isEmpty(); i++) {
            worldsToRemove.remove(0);
        }
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
}
