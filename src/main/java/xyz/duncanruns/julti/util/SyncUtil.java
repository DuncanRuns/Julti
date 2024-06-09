package xyz.duncanruns.julti.util;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static xyz.duncanruns.julti.Julti.log;

public final class SyncUtil {

    private SyncUtil() {
    }

    public static synchronized void sync(List<MinecraftInstance> instances, MinecraftInstance sourceInstance, Set<SyncOptions> syncOptions) throws IOException {
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
        for (SyncOptions o : syncOptions) {
            switch (o) {
                case MOD_CONFIGS:
                    syncModConfigs(sourcePath, destinationPaths);
                    break;
                case INSTANCE_SETTINGS:
                    syncInstanceSettings(sourcePath, destinationPaths);
                    break;
                case MODS:
                    syncMods(sourcePath, destinationPaths);
                    break;
                case GAME_OPTIONS:
                    syncOptions(sourcePath, destinationPaths);
                    break;
                case RESOURCE_PACKS:
                    syncResourcePacks(sourcePath, destinationPaths);
                    break;
            }
        }

        log(Level.INFO, "Sync finished!");
    }

    private static void syncMods(Path sourcePath, List<Path> destinationPaths) throws IOException {
        // mods/

        Path sourceModsPath = sourcePath.resolve("mods");
        if (!Files.isDirectory(sourceModsPath)) {
            log(Level.WARN, "Source instance has no mods folder!");
        }
        for (Path destinationPath : destinationPaths) {
            Path destinationModsPath = destinationPath.resolve("mods");
            log(Level.INFO, "Syncing mods: " + sourcePath + " -> " + destinationPath);
            FileUtils.deleteQuietly(destinationModsPath.toFile());
            FileUtils.copyDirectory(sourceModsPath.toFile(), destinationModsPath.toFile());
        }
    }

    private static void syncOptions(Path sourcePath, List<Path> destinationPaths) throws IOException {
        // options.txt

        Path sourceOptionsPath = sourcePath.resolve("options.txt");
        if (!Files.isRegularFile(sourceOptionsPath)) {
            log(Level.WARN, "Source instance has no options.txt!");
        }
        for (Path destinationPath : destinationPaths) {
            Path destinationOptionsPath = destinationPath.resolve("options.txt");
            destinationOptionsPath.toFile().delete();
            log(Level.INFO, "Syncing options.txt file: " + sourcePath + " -> " + destinationPath);
            Files.copy(sourceOptionsPath, destinationOptionsPath);
        }
    }

    private static void syncModConfigs(Path sourcePath, List<Path> destinationPaths) throws IOException {
        syncConfigFolder(sourcePath, destinationPaths);
        syncSpeedrunIGT(sourcePath, destinationPaths);
    }

    private static void syncSpeedrunIGT(Path sourcePath, List<Path> destinationPaths) throws IOException {
        // speedrunigt/

        Path sourceSpeedrunIGTPath = sourcePath.resolve("speedrunigt");
        if (!Files.isDirectory(sourceSpeedrunIGTPath)) {
            log(Level.WARN, "Source instance has no SpeedrunIGT config folder!");
            return;
        }
        for (Path destinationPath : destinationPaths) {
            Path destinationSpeedrunIGTPath = destinationPath.resolve("speedrunigt");
            log(Level.INFO, "Syncing SpeedrunIGT config folder: " + sourcePath + " -> " + destinationPath);
            FileUtils.deleteQuietly(destinationSpeedrunIGTPath.toFile());
            FileUtils.copyDirectory(sourceSpeedrunIGTPath.toFile(), destinationSpeedrunIGTPath.toFile());
        }
    }

    private static void syncConfigFolder(Path sourcePath, List<Path> destinationPaths) throws IOException {
        // config/

        Path sourceConfigPath = sourcePath.resolve("config");
        if (!Files.isDirectory(sourceConfigPath)) {
            log(Level.WARN, "Source instance has no config folder!");
            return;
        }
        for (Path destinationPath : destinationPaths) {
            Path destinationConfigPath = destinationPath.resolve("config");
            log(Level.INFO, "Syncing config folder: " + sourcePath + " -> " + destinationPath);
            FileUtils.deleteQuietly(destinationConfigPath.toFile());
            FileUtils.copyDirectory(sourceConfigPath.toFile(), destinationConfigPath.toFile());
        }
    }

    private static void syncResourcePacks(Path sourcePath, List<Path> destinationPaths) throws IOException {
        // resourcepacks

        Path sourceResourcePacksPath = sourcePath.resolve("resourcepacks");
        if (!Files.isDirectory(sourceResourcePacksPath)) {
            log(Level.WARN, "Source instance has no resource packs folder!");
        }
        for (Path destinationPath : destinationPaths) {
            Path destinationResourcePacksPath = destinationPath.resolve("resourcepacks");
            log(Level.INFO, "Syncing resource packs folder: " + sourcePath + " -> " + destinationPath);
            FileUtils.copyDirectory(sourceResourcePacksPath.toFile(), destinationResourcePacksPath.toFile());
        }
    }

    private static void syncInstanceSettings(Path sourcePath, List<Path> destinationPaths) throws IOException {
        syncInstanceCfg(sourcePath, destinationPaths);
        syncMMCPack(sourcePath, destinationPaths);
    }

    private static void syncInstanceCfg(Path sourcePath, List<Path> destinationPaths) throws IOException {
        // instance.cfg

        Path sourceInstanceCfgPath = sourcePath.getParent().resolve("instance.cfg");
        if (!Files.isRegularFile(sourceInstanceCfgPath)) {
            log(Level.WARN, "Source instance has no instance.cfg!");
            return;
        }
        Pattern nameLinePattern = Pattern.compile("(?m)^name=.+");
        String sourceContents = FileUtil.readString(sourceInstanceCfgPath);
        if (!nameLinePattern.matcher(sourceContents).find()) {
            log(Level.WARN, "Source instance has no instance.cfg with name= line!");
            return;
        }
        for (Path destinationPath : destinationPaths) {
            Path destinationInstanceCfgPath = destinationPath.getParent().resolve("instance.cfg");
            log(Level.INFO, "Syncing instance.cfg: " + sourcePath + " -> " + destinationPath);
            String destinationContents = FileUtil.readString(destinationInstanceCfgPath);
            Matcher matcher = nameLinePattern.matcher(destinationContents);
            if (!matcher.find()) {
                Julti.log(Level.WARN, "Can't copy instance.cfg for " + destinationPath + " because it has no name= line!");
                continue;
            }
            String nameLine = matcher.group();
            FileUtil.writeString(destinationInstanceCfgPath, nameLinePattern.matcher(sourceContents).replaceAll(nameLine));
        }
    }

    private static void syncMMCPack(Path sourcePath, List<Path> destinationPaths) throws IOException {
        // mmc-pack.json

        Path sourceMMCPackPath = sourcePath.getParent().resolve("mmc-pack.json");
        if (!Files.isRegularFile(sourceMMCPackPath)) {
            log(Level.WARN, "Source instance has no mmc-pack.json!");
            return;
        }
        for (Path destinationPath : destinationPaths) {
            Path destinationMMCPackPath = destinationPath.getParent().resolve("mmc-pack.json");
            destinationMMCPackPath.toFile().delete();
            log(Level.INFO, "Syncing mmc-pack.json file: " + sourcePath + " -> " + destinationPath);
            Files.copy(sourceMMCPackPath, destinationMMCPackPath);
        }
    }

    public enum SyncOptions {
        MODS, MOD_CONFIGS, GAME_OPTIONS, RESOURCE_PACKS, INSTANCE_SETTINGS
    }
}
