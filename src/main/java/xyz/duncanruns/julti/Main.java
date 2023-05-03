package xyz.duncanruns.julti;

import com.formdev.flatlaf.FlatDarkLaf;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.KeyboardUtil;

import javax.swing.*;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            runJultiApp();
        } catch (Exception e) {
            int ans = JOptionPane.showOptionDialog(null, "Julti has crashed during startup or main loop!", "Julti: Crash", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, new Object[]{"Copy Error", "Cancel"}, "Copy Error");
            if (ans == 0) {
                KeyboardUtil.copyToClipboard("Error during startup or main loop: " + e);
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
