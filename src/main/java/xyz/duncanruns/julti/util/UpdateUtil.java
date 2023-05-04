package xyz.duncanruns.julti.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import org.apache.logging.log4j.Level;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GitHub;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.Main;
import xyz.duncanruns.julti.gui.DownloadingJultiScreen;
import xyz.duncanruns.julti.gui.JultiGUI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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

            JultiOptions options = JultiOptions.getInstance();

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
            if (!Arrays.asList(Main.args).contains("--suggestUpdate")) {
                shouldSuggestUpdate = (!isAlreadyExactlyLatest) && isVersionGreater(latestVersionNums, currentVersionNums, canBeEqual) && isVersionGreater(latestVersionNums, getVersionNums(options.lastCheckedVersion), canBeEqual);
            }

            if (shouldSuggestUpdate) {
                options.lastCheckedVersion = latestVersion;
                if (JOptionPane.showConfirmDialog(gui, "A new update has been found!\nYou are on v" + Julti.VERSION + ", and the latest version is " + latestVersion + ".\nWould you like to update now?", "Julti: New Update!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == 0) {
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
            int ans = JOptionPane.showOptionDialog(null, "Julti has crashed during startup or main loop!", "Julti: Crash", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, new Object[]{"Copy Error", "Cancel"}, "Copy Error");
            if (ans == 0) {
                KeyboardUtil.copyToClipboard("Error during updating: " + e);
            }
            System.exit(1);
        }
    }

    private static void updateAndLaunch(GHAsset asset) throws IOException, PowerShellExecutionException, URISyntaxException {
        Point location = JultiGUI.getInstance().getLocation();
        JultiGUI.getInstance().closeForUpdate();

        if (!Files.exists(Paths.get(asset.getName()))) {
            DownloadingJultiScreen downloadingJultiScreen = new DownloadingJultiScreen(location);
            downloadingJultiScreen.download(asset);
        }

        Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("javaw.exe");

        // Use powershell's start-process to start it detached
        String powerCommand = String.format("start-process \"%s\" \"-jar %s -deleteOldJar %s\"", javaExe, asset.getName(), new File(UpdateUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getFileName().toString());
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
}
