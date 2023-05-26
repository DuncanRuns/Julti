package xyz.duncanruns.julti;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.KeyboardUtil;

import javax.swing.*;
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
        } catch (Exception exception) {
            String detailedException = ExceptionUtil.toDetailedString(exception);
            Julti.log(Level.ERROR, detailedException);
            int ans = JOptionPane.showOptionDialog(null, "Julti has crashed during startup or main loop!", "Julti: Crash", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, new Object[]{"Copy Error", "Cancel"}, "Copy Error");
            if (ans == 0) {
                KeyboardUtil.copyToClipboard("Error during startup or main loop: " + detailedException);
            }
            System.exit(1);
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

        // Run main loop
        Julti.getInstance().run();
    }
}
