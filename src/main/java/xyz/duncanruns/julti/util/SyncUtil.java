package xyz.duncanruns.julti.util;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static xyz.duncanruns.julti.Julti.log;

public final class SyncUtil {
    private static final Object LOCK = new Object();

    private SyncUtil() {
    }

    public static void sync(List<MinecraftInstance> instances, MinecraftInstance sourceInstance, boolean copyMods, boolean copyConfigs) throws IOException {
        synchronized (LOCK) {
            if (!copyConfigs && !copyMods) {
                return;
            }

            for (MinecraftInstance instance : instances) {
                if (instance.hasWindow()) {
                    log(Level.ERROR, "Cannot sync: one or more instances are still open!");
                    return;
                }
            }

            List<Path> destinationPaths = instances.stream().filter(instance -> !instance.equals(sourceInstance)).map(MinecraftInstance::getPath).collect(Collectors.toList());
            Path sourcePath = sourceInstance.getPath();

            if (destinationPaths.isEmpty()) {
                log(Level.WARN, "No instances to sync!");
                return;
            }
            log(Level.INFO, "Syncing Instances...");

            // mods/

            Path sourceModsPath = sourcePath.resolve("mods");
            if (copyMods && Files.isDirectory(sourceModsPath)) {
                for (Path destinationPath : destinationPaths) {
                    Path destinationModsPath = destinationPath.resolve("mods");
                    log(Level.INFO, "Syncing mods: " + sourcePath + " -> " + destinationPath);
                    FileUtils.deleteQuietly(destinationModsPath.toFile());
                    FileUtils.copyDirectory(sourceModsPath.toFile(), destinationModsPath.toFile());
                }
            } else if (copyMods) {
                log(Level.WARN, "Source instance has no mods folder!");
            }

            // options.txt

            if (copyConfigs) {
                Path sourceOptionsPath = sourcePath.resolve("options.txt");
                if (Files.isRegularFile(sourceOptionsPath)) {
                    for (Path destinationPath : destinationPaths) {
                        Path destinationOptionsPath = destinationPath.resolve("options.txt");
                        destinationOptionsPath.toFile().delete();
                        log(Level.INFO, "Syncing options.txt file: " + sourcePath + " -> " + destinationPath);
                        FileUtils.copyFile(sourceOptionsPath.toFile(), destinationOptionsPath.toFile());
                    }
                } else {
                    log(Level.WARN, "Source instance has no options.txt!");
                }
            }

            // config/

            Path sourceConfigPath = sourcePath.resolve("config");
            if (copyConfigs && Files.isDirectory(sourceConfigPath)) {
                for (Path destinationPath : destinationPaths) {
                    Path destinationConfigPath = destinationPath.resolve("config");
                    log(Level.INFO, "Syncing config folder: " + sourcePath + " -> " + destinationPath);
                    FileUtils.deleteQuietly(destinationConfigPath.toFile());
                    FileUtils.copyDirectory(sourceConfigPath.toFile(), destinationConfigPath.toFile());
                }
            } else if (copyConfigs) {
                log(Level.WARN, "Source instance has no config folder!");
            }

            // speedrunigt/

            Path sourceSpeedrunIGTPath = sourcePath.resolve("speedrunigt");
            if (copyConfigs && Files.isDirectory(sourceSpeedrunIGTPath)) {
                for (Path destinationPath : destinationPaths) {
                    Path destinationSpeedrunIGTPath = destinationPath.resolve("speedrunigt");
                    log(Level.INFO, "Syncing SpeedrunIGT config folder: " + sourcePath + " -> " + destinationPath);
                    FileUtils.deleteQuietly(destinationSpeedrunIGTPath.toFile());
                    FileUtils.copyDirectory(sourceSpeedrunIGTPath.toFile(), destinationSpeedrunIGTPath.toFile());
                }
            } else if (copyConfigs) {
                log(Level.WARN, "Source instance has no SpeedrunIGT config folder!");
            }

            log(Level.INFO, "Sync finished!");
        }
    }
}
