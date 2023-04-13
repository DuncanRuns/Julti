package xyz.duncanruns.julti;

import com.formdev.flatlaf.FlatDarkLaf;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.script.ScriptManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
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
