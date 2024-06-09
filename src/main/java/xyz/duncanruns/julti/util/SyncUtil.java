package xyz.duncanruns.julti.util;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static xyz.duncanruns.julti.Julti.log;

public final class SyncUtil {

    private SyncUtil() {
    }

    public static Optional<Set<SyncOptions>> ask() {
        boolean instancesOpen = InstanceManager.getInstanceManager().getInstances().stream().anyMatch(MinecraftInstance::hasWindow);
        JCheckBox modsBox = getBoxForClosed("Mods", instancesOpen, true);
        JCheckBox modConfigsBox = getBox("Mod Configs", true);
        JCheckBox gameOptionsBox = getBoxForClosed("Game Options", instancesOpen, true);
        JCheckBox instanceSettingsBox = getBoxForClosed("Instance Settings", instancesOpen, true);
        JCheckBox resourcePacksBox = getBox("Resource Packs", false);

        Frame frame = JultiGUI.getJultiGUI();
        JDialog dialog = new JDialog(frame, "Sync Instances", true);
        dialog.setSize(250, instancesOpen ? 200 : 150);
        dialog.setLocationRelativeTo(frame);
        if (instancesOpen) {
            // Java swing will bend to my will
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout());
            JPanel panel2 = new JPanel();
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
            panel2.add(new JLabel("One or more instances are open"));
            panel2.add(new JLabel("so some options are not accessible!"));
            panel.add(panel2);
            dialog.add(panel, BorderLayout.NORTH);
        }

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(modsBox);
        panel.add(modConfigsBox);
        panel.add(gameOptionsBox);
        panel.add(instanceSettingsBox);
        panel.add(resourcePacksBox);

        dialog.add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        AtomicBoolean confirmed = new AtomicBoolean(false);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            dialog.dispose();
            confirmed.set(true);
        });
        buttonPanel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);

        if (!confirmed.get()) {
            return Optional.empty();
        }
        Set<SyncOptions> options = new HashSet<>();

        // check again in case an instance opened during the dialog
        if (InstanceManager.getInstanceManager().getInstances().stream().noneMatch(MinecraftInstance::hasWindow)) {
            if (modsBox.isSelected()) {
                options.add(SyncOptions.MODS);
            }
            if (gameOptionsBox.isSelected()) {
                options.add(SyncOptions.GAME_OPTIONS);
            }
            if (instanceSettingsBox.isSelected()) {
                options.add(SyncOptions.INSTANCE_SETTINGS);
            }
        }
        if (modConfigsBox.isSelected()) {
            options.add(SyncOptions.MOD_CONFIGS);
        }
        if (resourcePacksBox.isSelected()) {
            options.add(SyncOptions.RESOURCE_PACKS);
        }

        return Optional.of(options);
    }

    private static JCheckBox getBox(String name, boolean def) {
        JCheckBox b = new JCheckBox();
        b.setText(name);
        b.setSelected(def);
        return b;
    }

    private static JCheckBox getBoxForClosed(String name, boolean instancesOpen, boolean def) {
        JCheckBox b = new JCheckBox();
        b.setText(name);
        if (instancesOpen) {
            b.setEnabled(false);
        } else {
            b.setSelected(def);
        }
        return b;
    }

    public static synchronized void sync(List<MinecraftInstance> instances, MinecraftInstance sourceInstance, Set<SyncOptions> syncOptions) throws IOException {
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
            FileUtils.deleteDirectory(destinationModsPath.toFile());
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
            FileUtils.deleteDirectory(destinationSpeedrunIGTPath.toFile());
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
            FileUtils.deleteDirectory(destinationConfigPath.toFile());
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
            Files.list(destinationResourcePacksPath).forEach(path -> {
                if (Files.isDirectory(path)) {
                    try {
                        FileUtils.deleteDirectory(path.toFile());
                    } catch (IOException ignored) {
                    }
                } else {
                    path.toFile().delete();
                }
            });
            Files.list(sourceResourcePacksPath).forEach(sourceResourcePackPath -> {
                Path destinationResourcePackPath = destinationResourcePacksPath.resolve(sourceResourcePackPath.getFileName().toString());
                try {
                    if (Files.isDirectory(sourceResourcePackPath)) {
                        FileUtils.copyDirectory(sourceResourcePackPath.toFile(), destinationResourcePackPath.toFile());
                    } else {
                        Files.copy(sourceResourcePackPath, destinationResourcePackPath);
                    }
                } catch (IOException e) {
                    Julti.log(Level.WARN, "Can't replace \"" + destinationResourcePackPath + "\", the instance is probably open");
                }
            });
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
