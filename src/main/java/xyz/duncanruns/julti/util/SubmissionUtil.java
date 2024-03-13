package xyz.duncanruns.julti.util;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

        // save submission to folder
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String submissionFolderName = (instance.getName() + " submission (" + dtf.format(now) + ")")
                .replace(":", "-")
                .replace("/", "-");

        Path submissionPath = Paths.get(System.getProperty("user.home"))
                .resolve(".Julti")
                .resolve("submissionpackages")
                .resolve(submissionFolderName
                );
        submissionPath.toFile().mkdirs();
        Julti.log(Level.INFO, "Created folder for submission.");

        // latest world + 5 previous saves
        List<Path> worldsToCopy = Arrays.stream(Objects.requireNonNull(savesPath.toFile().list())) // Get all world names
                .map(savesPath::resolve) // Map to world paths
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
        Path savesDest = submissionPath.resolve("Worlds");
        savesDest.toFile().mkdirs();
        try {
            for (Path currentPath : worldsToCopy.subList(0, Math.min(worldsToCopy.size(), 6))) {
                File currentSave = currentPath.toFile();
                Julti.log(Level.INFO, "Copying " + currentSave.getName() + " to submission folder...");
                FileUtils.copyDirectoryToDirectory(currentSave, savesDest.toFile());
            }
        } catch (FileSystemException e) {
            String message = "Cannot package files - a world appears to be open! Please press Options > Stop Resets & Quit in your instance.";
            JOptionPane.showMessageDialog(JultiGUI.getJultiGUI(), message, "Julti: Package Files Error", JOptionPane.ERROR_MESSAGE);
            Julti.log(Level.ERROR, message);
            return null;
        }

        // last 3 logs
        List<Path> logsToCopy = Arrays.stream(Objects.requireNonNull(logsPath.toFile().list())) // Get all log names
                .map(logsPath::resolve) // Map to paths
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
        File logsDest = submissionPath.resolve("Logs").toFile();
        logsDest.mkdirs();
        for (Path currentPath : logsToCopy.subList(0, Math.min(logsToCopy.size(), 6))) {
            File currentLog = currentPath.toFile();
            Julti.log(Level.INFO, "Copying " + currentLog.getName() + " to submission folder...");
            FileUtils.copyFileToDirectory(currentLog, logsDest);
        }

        Julti.log(Level.INFO, "Saved submission files for " + instance.getName() + " to .Julti/submissionpackages.\r\nPlease submit a download link to your files through this form: https://forms.gle/v7oPXfjfi7553jkp7");

        copyFolderToZip(submissionPath.resolve("Worlds.zip"), submissionPath.resolve("Worlds"));
        copyFolderToZip(submissionPath.resolve("Logs.zip"), submissionPath.resolve("Logs"));

        return submissionPath;
    }

    private static void copyFolderToZip(Path zipFile, Path sourceFolder) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walkFileTree(sourceFolder, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new ZipFileVisitor(zos, sourceFolder));
        } catch (IOException e) {
            Julti.log(Level.ERROR, "Error while copying zip to folder: \n" + ExceptionUtil.toDetailedString(e));
        }
    }

    private static class ZipFileVisitor extends java.nio.file.SimpleFileVisitor<Path> {

        private final ZipOutputStream zos;
        private final Path sourceFolder;

        public ZipFileVisitor(ZipOutputStream zos, Path sourceFolder) {
            this.zos = zos;
            this.sourceFolder = sourceFolder;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path relativePath = this.sourceFolder.relativize(file);
            this.zos.putNextEntry(new ZipEntry(relativePath.toString()));
            Files.copy(file, this.zos);
            this.zos.closeEntry();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
