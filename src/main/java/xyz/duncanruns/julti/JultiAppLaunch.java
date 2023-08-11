package xyz.duncanruns.julti;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.io.IOException;
import java.util.Arrays;

public final class JultiAppLaunch {
    public static String[] args;

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

    private static void runJultiApp() throws IOException {
        // Setup GUI theme
        FlatDarkLaf.setup();

        // Load Options
        JultiOptions.getJultiOptions();
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

        // Run main loop
        Julti.getJulti().run();
    }
}
