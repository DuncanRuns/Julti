package xyz.duncanruns.julti.util;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.github.GitHub;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.gui.JultiGUI;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.Arrays;

public final class UpdateUtil {
    private static final Logger LOGGER = LogManager.getLogger("UpdateChecker");

    private UpdateUtil() {
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        checkForUpdates(null);
    }

    public static void checkForUpdates(JultiGUI gui) {
        Thread.currentThread().setName("update-checker");
        try {
            if (Julti.VERSION.equals("DEV")) {
                log(Level.INFO, "No updates because Julti is in DEV version.");
                return;
            }

            JultiOptions options = JultiOptions.getInstance();

            // Get the version tag of the first found release (always latest) of the list of releases of the Julti repository from an anonymous GitHub connection.
            String latestVersion = GitHub.connectAnonymously().getRepository("DuncanRuns/Julti").listReleases().toList().get(0).getTagName();

            // Convert the latest version and current version to ints
            int[] latestVersionNums = getVersionNums(latestVersion);
            int[] currentVersionNums = getVersionNums(Julti.VERSION);

            // latestVersion usually starts with "v", Julti.VERSION does not, so using endswith basically checks equals ignoring the v
            boolean isAlreadyExactlyLatest = latestVersion.endsWith(Julti.VERSION);
            boolean canBeEqual = Julti.VERSION.contains("-") || Julti.VERSION.contains("+");

            if ((!isAlreadyExactlyLatest) && isVersionGreater(latestVersionNums, currentVersionNums, canBeEqual) && isVersionGreater(latestVersionNums, getVersionNums(options.lastCheckedVersion), canBeEqual)) {
                options.lastCheckedVersion = latestVersion;
                if (JOptionPane.showConfirmDialog(gui, "A new update has been found!\nYou are on v" + Julti.VERSION + ", and the latest version is " + latestVersion + ".\nWould you like to go to the releases page?", "Julti: New Update!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == 0) {
                    Desktop.getDesktop().browse(new URI("https://github.com/DuncanRuns/Julti/releases"));
                }
                return;
            }
            log(Level.INFO, "No new updates found!");

        } catch (Exception e) {
            log(Level.WARN, "Update check failed! Maybe you are not connected to the internet.");
        }
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }

    private static int[] getVersionNums(String versionString) {
        // Remove v prefix
        versionString = versionString.startsWith("v") ? versionString.substring(1) : versionString;
        // Remove suffix
        for (char c : new char[]{'+', '-'}) {
            if (versionString.contains("" + c)) {
                versionString = versionString.substring(0, versionString.indexOf(c));
            }
        }
        return Arrays.stream(versionString.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    private static boolean isVersionGreater(int[] latestVersionNums, int[] currentVersionNums, boolean canBeEqual) {
        boolean isGreater = false;
        for (int i = 2; i >= 0; i--) {
            if (i == 2 && canBeEqual) {
                if (latestVersionNums[i] >= currentVersionNums[i]) isGreater = true;
            } else {
                if (latestVersionNums[i] > currentVersionNums[i]) isGreater = true;
            }
            if (latestVersionNums[i] < currentVersionNums[i]) isGreater = false;
        }
        return isGreater;
    }
}
