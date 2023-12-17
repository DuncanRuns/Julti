package xyz.duncanruns.julti.util;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SubmissionUtil {
    private SubmissionUtil() {
    }

    /**
     * Returns the path of the prepared submission folder or null if failed.
     */
    public static Path tryPrepareSubmission(MinecraftInstance instance) {
        try {
            return prepareSubmission(instance);
        } catch (IOException | SecurityException e) {
            Julti.log(Level.ERROR, "Failed to prepare files for submission - please refer to the speedrun.com rules to submit files yourself.\nDetailed error:" + ExceptionUtil.toDetailedString(e));
            return null;
        }
    }

    /**
     * @author DuncanRuns
     * @author draconix6
     */
    public static Path prepareSubmission(MinecraftInstance instance) throws IOException, SecurityException {
        Path savesPath = instance.getPath().resolve("saves");
        if (!Files.isDirectory(savesPath)) {
            Julti.log(Level.ERROR, "Saves path for " + instance.getName() + " not found! Please refer to the speedrun.com rules to submit files yourself.");
            return null;
        }

        Path logsPath = instance.getPath().resolve("logs");
        if (!Files.isDirectory(logsPath)) {
            Julti.log(Level.ERROR, "Logs path for " + instance.getName() + " not found! Please refer to the speedrun.com rules to submit files yourself.");
            return null;
        }

        // save submission to folder on desktop
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        Path submissionPath = Paths.get(System.getProperty("user.home"))
                .resolve(".Julti")
                .resolve("submissionpackages")
                .resolve((instance.getName() + " submission (" + dtf.format(now) + ")")
                        .replace(":", "-")
                        .replace("/", "-")
                );
        submissionPath.toFile().mkdirs();
        Julti.log(Level.INFO, "Created folder on desktop for submission.");

        // latest world + 5 previous saves
        List<Path> worldsToCopy = Arrays.stream(Objects.requireNonNull(savesPath.toFile().list())) // Get all world names
                .map(savesPath::resolve) // Map to world paths
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
        File savesDest = submissionPath.resolve("Worlds").toFile();
        savesDest.mkdirs();
        try {
            for (int i = 0; i < 6; i++) {
                File currentSave = worldsToCopy.get(i).toFile();
                Julti.log(Level.INFO, "Copying " + currentSave.getName() + " to Desktop...");
                FileUtils.copyDirectoryToDirectory(currentSave, savesDest);
            }
        } catch (IndexOutOfBoundsException ignored) {
        } // not enough saves to copy

        // last 3 logs
        List<Path> logsToCopy = Arrays.stream(Objects.requireNonNull(logsPath.toFile().list())) // Get all log names
                .map(logsPath::resolve) // Map to paths
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
        File logsDest = submissionPath.resolve("Logs").toFile();
        logsDest.mkdirs();
        try {
            for (int i = 0; i < 3; i++) {
                File currentLog = logsToCopy.get(i).toFile();
                Julti.log(Level.INFO, "Copying " + currentLog.getName() + " to Desktop...");
                FileUtils.copyFileToDirectory(currentLog, logsDest);
            }
        } catch (IndexOutOfBoundsException ignored) {
        } // not enough logs to copy

        Julti.log(Level.INFO, "Saved submission files for " + instance.getName() + " to .Julti/submissionpackages.\r\nPlease submit a link to your files through instance form: https://forms.gle/v7oPXfjfi7553jkp7");

        return submissionPath;
    }
}
