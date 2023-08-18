package xyz.duncanruns.julti.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import org.apache.logging.log4j.Level;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GitHub;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.gui.DownloadProgressFrame;
import xyz.duncanruns.julti.gui.JultiGUI;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static xyz.duncanruns.julti.Julti.log;

public final class UpdateUtil {

    private UpdateUtil() {
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        checkForUpdates(null);
        System.exit(0);
    }

    public static void checkForUpdates(JultiGUI gui) {
        try {
            if (Julti.VERSION.equals("DEV")) {
                log(Level.INFO, "No updates because Julti is in DEV version.");
                return;
            }

            JultiOptions options = JultiOptions.getJultiOptions();

            // Get the version tag of the first found release (always latest) of the list of releases of the Julti repository from an anonymous GitHub connection.
            GHRelease release = GitHub.connectAnonymously().getRepository("DuncanRuns/Julti").listReleases().toList().get(0);
            GHAsset asset = null;
            for (GHAsset listAsset : release.listAssets()) {
                if (listAsset.getBrowserDownloadUrl().endsWith(".jar")) {
                    asset = listAsset;
                    break;
                }
            }
            String latestVersion = release.getTagName();

            if (asset == null || asset.getBrowserDownloadUrl() == null) {
                Julti.log(Level.WARN, "Latest github release does not have a .jar asset! Please report this to the developer.");
            }

            // Convert the latest version and current version to ints
            int[] latestVersionNums = getVersionNums(latestVersion);
            int[] currentVersionNums = getVersionNums(Julti.VERSION);

            // latestVersion usually starts with "v", Julti.VERSION does not, so using endswith basically checks equals ignoring the v
            boolean isAlreadyExactlyLatest = latestVersion.endsWith(Julti.VERSION);
            boolean canBeEqual = Julti.VERSION.contains("-") || Julti.VERSION.contains("+");

            boolean shouldSuggestUpdate = true;
            if (!Arrays.asList(JultiAppLaunch.args).contains("--suggestUpdate")) {
                shouldSuggestUpdate = (!isAlreadyExactlyLatest) && isVersionGreater(latestVersionNums, currentVersionNums, canBeEqual) && isVersionGreater(latestVersionNums, getVersionNums(options.lastCheckedVersion), canBeEqual);
            }

            if (shouldSuggestUpdate) {
                options.lastCheckedVersion = latestVersion;
                if (JOptionPane.showConfirmDialog(gui, "A new update has been found!\nYou are on v" + Julti.VERSION + ", and the latest version is " + latestVersion + ".\nWould you like to update now?", "Julti: New Update!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == 0) {
                    if (Julti.isRanFromAlternateLocation()) {
                        JOptionPane.showMessageDialog(gui, "Julti has detected that it is being ran from an alternate location. If you are using a shortcut, you will likely need to recreate it once the update has finished.", "Julti: Ran from Alternate Location", JOptionPane.INFORMATION_MESSAGE);
                    }
                    // Desktop.getDesktop().browse(new URI("https://github.com/DuncanRuns/Julti/releases"));
                    tryUpdateAndLaunch(asset);
                }
                return;
            }
            log(Level.INFO, "No new updates found!");

        } catch (Exception e) {
            log(Level.WARN, "Update check failed! Maybe you are not connected to the internet.");
            log(Level.WARN, "Update exception: " + e);
        }
    }

    public static void tryUpdateAndLaunch(GHAsset asset) {
        try {
            updateAndLaunch(asset);
        } catch (Exception e) {
            ExceptionUtil.showExceptionAndExit(e, "Julti has crashed during an update!");
        }
    }

    private static void updateAndLaunch(GHAsset asset) throws IOException, PowerShellExecutionException {
        Path newJarPath = Julti.getSourcePath().resolveSibling(asset.getName());

        Point location = JultiGUI.getJultiGUI().getLocation();
        JultiGUI.getJultiGUI().closeForUpdate();

        if (!Files.exists(newJarPath)) {
            downloadAssetWithProgress(asset, newJarPath, new DownloadProgressFrame(location).getBar());
        }

        Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("javaw.exe");

        // Use powershell's start-process to start it detached
        String powerCommand = String.format("start-process '%s' '-jar \"%s\" -deleteOldJar \"%s\"'", javaExe, newJarPath, Julti.getSourcePath());
        Julti.log(Level.INFO, "Exiting and running powershell command: " + powerCommand);
        PowerShellUtil.execute(powerCommand);

        System.exit(0);
    }

    private static int[] getVersionNums(String versionString) {
        if (versionString.equals("DEV")) {
            return new int[]{0, 0, 0};
        }

        // Remove v prefix
        versionString = versionString.startsWith("v") ? versionString.substring(1) : versionString;
        // Remove suffix
        for (char c : new char[]{'+', '-'}) {
            if (versionString.contains(String.valueOf(c))) {
                versionString = versionString.substring(0, versionString.indexOf(c));
            }
        }
        return Arrays.stream(versionString.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    private static boolean isVersionGreater(int[] latestVersionNums, int[] currentVersionNums, boolean canBeEqual) {
        boolean isGreater = false;
        for (int i = 2; i >= 0; i--) {
            if (i == 2 && canBeEqual) {
                if (latestVersionNums[i] >= currentVersionNums[i]) {
                    isGreater = true;
                }
            } else {
                if (latestVersionNums[i] > currentVersionNums[i]) {
                    isGreater = true;
                }
            }
            if (latestVersionNums[i] < currentVersionNums[i]) {
                isGreater = false;
            }
        }
        return isGreater;
    }

    public static void downloadAssetWithProgress(GHAsset asset, Path newJarPath, JProgressBar bar) throws IOException {
        URL url = new URL(asset.getBrowserDownloadUrl());
        URLConnection connection = url.openConnection();
        connection.connect();
        int fileSize = connection.getContentLength();
        bar.setMaximum(fileSize);
        bar.setValue(0);
        int i = 0;
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(newJarPath.toFile())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                i += bytesRead;
                if (i >= 102400) {
                    bar.setValue(bar.getValue() + i);
                    i = 0;
                }
            }
        }
    }
}
