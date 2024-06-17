package xyz.duncanruns.julti.util;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiAppLaunch;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.gui.DownloadProgressFrame;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.plugin.PluginEvents;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static xyz.duncanruns.julti.Julti.log;

public final class UpdateUtil {
    private static JsonObject meta = null;

    private UpdateUtil() {
    }

    public static void tryCheckForUpdates(JultiGUI gui) {
        tryCheckForUpdates(gui, null);
    }

    public static void tryCheckForUpdates(JultiGUI gui, String currentVersion) {
        try {
            checkForUpdates(gui, currentVersion);

        } catch (Exception e) {
            log(Level.WARN, "Update check failed! Maybe you are not connected to the internet, or GitHub could be rate limiting you.");
            log(Level.WARN, "Update exception: " + ExceptionUtil.toDetailedString(e));
        }
    }

    private synchronized static void checkForUpdates(JultiGUI gui, String currentVersion) throws IOException {
        if (currentVersion == null) {
            currentVersion = Julti.VERSION;
        }
        currentVersion = currentVersion.replaceAll("^v", "");
        if (currentVersion.equals("DEV")) {
            log(Level.INFO, "No updates because Julti is in DEV version.");
            return;
        }

        JultiOptions options = JultiOptions.getJultiOptions();
        if (meta == null) {
            meta = GrabUtil.grabJson("https://raw.githubusercontent.com/DuncanRuns/Julti/main/meta.json");
        }
        Julti.log(Level.DEBUG, "Grabbed Meta: " + meta.toString());

        String latestVersion = meta.get(options.usePreReleases ? "latest_dev" : "latest").getAsString();
        String download = meta.get(options.usePreReleases ? "latest_dev_download" : "latest_download").getAsString();
        boolean matchesLatest = latestVersion.equals(currentVersion);

        if (!matchesLatest && !options.lastCheckedVersion.equals(latestVersion)) {
            options.lastCheckedVersion = latestVersion;
            if (JOptionPane.showConfirmDialog(gui, "A new update has been found!\nYou are on v" + currentVersion + ", and the latest version is " + latestVersion + ".\nWould you like to update now?", "Julti: New Update!", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) == 0) {
                if (Julti.isRanFromAlternateLocation()) {
                    JOptionPane.showMessageDialog(gui, "Julti has detected that it is being ran from an alternate location. If you are using a shortcut, you will likely need to recreate it once the update has finished.", "Julti: Ran from Alternate Location", JOptionPane.INFORMATION_MESSAGE);
                }
                // Desktop.getDesktop().browse(new URI("https://github.com/DuncanRuns/Julti/releases"));
                tryUpdateAndLaunch(download);
            }
        } else {
            log(Level.INFO, "No new updates found!");
        }
    }

    public static void tryUpdateAndLaunch(String download) {
        try {
            updateAndLaunch(download);
        } catch (Exception e) {
            ExceptionUtil.showExceptionAndExit(e, "Julti has crashed during an update!");
        }
    }

    private static void updateAndLaunch(String download) throws IOException, PowerShellExecutionException {
        Path newJarPath = Julti.getSourcePath().resolveSibling(URLDecoder.decode(FilenameUtils.getName(download), StandardCharsets.UTF_8.name()));

        Point location = JultiGUI.getJultiGUI().getLocation();
        JultiGUI.getJultiGUI().closeForUpdate();

        if (!Files.exists(newJarPath)) {
            downloadWithProgress(download, newJarPath, new DownloadProgressFrame(location).getBar());
        }

        Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("javaw.exe");

        PluginEvents.RunnableEventType.PRE_UPDATE.runAll();
        // Release LOCK so updating can go smoothly
        JultiAppLaunch.releaseLock();
        JultiOptions.getJultiOptions().trySave();

        // Use powershell's start-process to start it detached
        String powerCommand = String.format("start-process '%s' '-jar \"%s\" -deleteOldJar \"%s\"'", javaExe, newJarPath, Julti.getSourcePath());
        Julti.log(Level.INFO, "Exiting and running powershell command: " + powerCommand);
        PowerShellUtil.execute(powerCommand);

        System.exit(0);
    }

    public static void downloadWithProgress(String download, Path newJarPath, JProgressBar bar) throws IOException {
        URL url = new URL(download);
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
