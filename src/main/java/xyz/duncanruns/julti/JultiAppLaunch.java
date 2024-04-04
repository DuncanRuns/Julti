package xyz.duncanruns.julti;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.plugin.PluginInitializer;
import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.ExceptionUtil;

import javax.swing.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class JultiAppLaunch {
    public static String[] args;
    private static RandomAccessFile file;
    private static FileLock lock;

    private JultiAppLaunch() {
    }

    public static void main(String[] args) {
        JultiAppLaunch.args = args;
        System.out.println("Launched with args: " + Arrays.toString(args));

        try {
            runJultiApp();
        } catch (Exception e) {
            ExceptionUtil.showExceptionAndExit(e, "Julti has crashed during startup or main loop!");
        }
    }

    public static void launchWithDevPlugin(String[] args, PluginManager.JultiPluginData pluginData, PluginInitializer pluginInitializer) {
        PluginManager.getPluginManager().registerPlugin(pluginData, pluginInitializer);
        main(args);
    }

    private static void runJultiApp() throws IOException {
        // Set Swing Settings
        setSwingSettings();

        // Ensure Julti Dir
        JultiOptions.ensureJultiDir();

        // Check lock
        tryCheckLock();

        // Load Options
        JultiOptions.getJultiOptions();

        // Reload Scripts
        ScriptManager.reload();

        // Start Affinity Manager
        AffinityManager.start();

        // Start GUI
        JultiGUI.getJultiGUI().setVisible();

        // Start hotkey checker
        HotkeyManager.getHotkeyManager().start();

        // Redirect uncaught exceptions to Julti logging
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Julti.log(Level.ERROR, "Uncaught exception in thread " + t + ":\n" + ExceptionUtil.toDetailedString(e));
        });

        // Load Plugins
        PluginManager.getPluginManager().loadPlugins();

        // Initialize Plugins
        PluginManager.getPluginManager().initializePlugins();

        // Run main loop
        Julti.getJulti().run();
    }

    private static void setSwingSettings() {
        // Setup GUI theme
        FlatDarkLaf.setup();

        // Set tooltip delay
        ToolTipManager.sharedInstance().setInitialDelay(0);
    }

    /**
     * Tries to get the file lock on the .Julti/LOCK file, and if an IOException is thrown, asks the user if they would like to continue.
     */
    private static void tryCheckLock() {
        try {
            checkLock();
        } catch (IOException e) {
            showMultiJultiWarning();
            scheduleRegainLock();
        }
    }

    /**
     * Tries to obtain the lock every few seconds so that when an old Julti closes, this Julti can obtain the lock.
     */
    private static void scheduleRegainLock() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                checkLock();
            } catch (IOException e) {
                scheduleRegainLock();
            }
        }, 2, TimeUnit.SECONDS);
    }

    /**
     * Tries to get the file lock on the .Julti/LOCK file.
     *
     * @throws IOException if the lock could not be obtained.
     */
    private static void checkLock() throws IOException {
        Path lockPath = JultiOptions.getJultiDir().resolve("LOCK").toAbsolutePath();

        // Create random access file and file lock and store in fields to not get gc'd.
        file = new RandomAccessFile(lockPath.toFile(), "rw");
        lock = file.getChannel().tryLock();
        file.write('0'); // throws IOException when already locked

        if (lock == null) {
            throw new IOException("LOCK file is locked!");
        }

        // Schedule lock release
        Runtime.getRuntime().addShutdownHook(new Thread(JultiAppLaunch::releaseLock));
    }

    private static void showMultiJultiWarning() {
        if (0 != JOptionPane.showConfirmDialog(null, "Julti is already running! Are you sure you want to open Julti again?", "Julti: Already Opened", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
            System.exit(0);
        }
    }

    public static void releaseLock() {
        try {
            lock.release();
            file.close();
        } catch (IOException ignored) {
        }
    }
}