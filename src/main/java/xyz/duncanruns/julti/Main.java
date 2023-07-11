package xyz.duncanruns.julti;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.util.Arrays;

public final class Main {
    public static String[] args;

    private Main() {
    }

    public static void main(String[] args) {
        Main.args = args;
        System.out.println("Launched with args: " + Arrays.toString(args));

        try {
            runJultiApp();
        } catch (Exception e) {
            ExceptionUtil.showExceptionAndExit(e, "Julti has crashed during startup or main loop!");
        }
    }

    private static void runJultiApp() {
        // Setup GUI theme
        FlatDarkLaf.setup();

        // Load Options
        JultiOptions.getInstance();
        ScriptManager.reload();

        // Start Affinity Manager
        AffinityManager.start();

        // Start GUI
        JultiGUI.getInstance().setVisible();

        // Start hotkey checker
        HotkeyManager.getInstance().start();

        // Redirect uncaught exceptions to Julti logging
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Julti.log(Level.ERROR, "Uncaught exception in thread " + t + ":\n" + ExceptionUtil.toDetailedString(e));
        });

        // Run main loop
        Julti.getInstance().run();
    }
}
