package xyz.duncanruns.julti.util;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import org.apache.logging.log4j.Level;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GitHub;
import xyz.duncanruns.julti.Julti;
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

import static xyz.duncanruns.julti.Julti.log;

public final class UpdateUtil {

    private UpdateUtil() {
    }

    public static void tryCheckForUpdates(JultiGUI gui) {
        tryCheckForUpdates(gui, null);
    }

    public static void tryCheckForUpdates(JultiGUI gui, String currentVersion) {
        try {
            checkForUpdates(gui, currentVersion);

        } catch (Exception e) {
            log(Level.WARN, "Update check failed! Maybe you are not connected to the internet.");
            log(Level.WARN, "Update exception: " + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void checkForUpdates(JultiGUI gui, String currentVersion) throws IOException {
        if (currentVersion == null) {
            currentVersion = Julti.VERSION;
        }
        if (currentVersion.equals("DEV")) {
            log(Level.INFO, "No updates because Julti is in DEV version.");
            return;
        }

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

        checkForUpdates(gui, currentVersion, latestVersion, asset);
    }

    private static void checkForUpdates(JultiGUI gui, String currentVersion, String latestVersion, GHAsset asset) {
        currentVersion = currentVersion.replaceAll("^v", "");
        latestVersion = latestVersion.replaceAll("^v", "");
        JultiOptions options = JultiOptions.getJultiOptions();

        boolean shouldSuggestUpdate = !options.lastCheckedVersion.equals(latestVersion) && shouldUpdate(currentVersion, latestVersion);

        if (shouldSuggestUpdate) {
            options.lastCheckedVersion = latestVersion;
            if (JOptionPane.showConfirmDialog(gui, "A new update has been found!\nYou are on v" + currentVersion + ", and the latest version is " + latestVersion + ".\nWould you like to update now?", "Julti: New Update!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == 0) {
                if (Julti.isRanFromAlternateLocation()) {
                    JOptionPane.showMessageDialog(gui, "Julti has detected that it is being ran from an alternate location. If you are using a shortcut, you will likely need to recreate it once the update has finished.", "Julti: Ran from Alternate Location", JOptionPane.INFORMATION_MESSAGE);
                }
                // Desktop.getDesktop().browse(new URI("https://github.com/DuncanRuns/Julti/releases"));
                tryUpdateAndLaunch(asset);
            }
        } else {
            log(Level.INFO, "No new updates found!");
        }
    }

    private static boolean shouldUpdate(String currentVersion, String latestVersion) {
        if (latestVersion.equals(currentVersion)) {
            return false;
        }
        int compare = VersionUtil.compare(currentVersion.split("\\+")[0], latestVersion.split("\\+")[0]);
        if (compare < 0) { // If current version is older
            // If latest version is not a pre-release or current is a pre-release, it should update
            // This covers updating from a pre-release to any type of new version, and full releases to newer full releases,
            // but does not allow full releases to pre releases.
            return (!isPreRelease(latestVersion)) || (isPreRelease(currentVersion));
        } else if (compare == 0) { // If versions are loosely the same
            // If latest version is not a pre-release or both current and latest are pre releases, it should update.
            // This covers pre-release updating to another pre-release of the same upcoming version, or pre-release to the final version.
            return (!isPreRelease(latestVersion)) || (isPreRelease(latestVersion) && isPreRelease(currentVersion));
        }
        return false;
    }

    private static boolean isPreRelease(String versionString) {
        return versionString.contains("+pre");
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
