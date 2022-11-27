package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatMarginBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.SafeInstanceLauncher;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    private final Julti julti;
    private OptionsGUI optionsGUI = null;

    public ControlPanel(Julti julti, JultiGUI gui) {
        this.julti = julti;
        setLayout(new GridLayout(0, 1, 5, 5));
        setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));
        add(GUIUtil.getButtonWithMethod(new JButton("Redetect Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            if (0 == JOptionPane.showConfirmDialog(gui, "This will remove all instances saved to the profile and replace them with new ones.\nAre you sure you want to do this?", "Julti: Redetect Instances", JOptionPane.WARNING_MESSAGE))
                julti.redetectInstances();
        }));
        add(GUIUtil.getButtonWithMethod(new JButton("Set Titles"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            julti.getInstanceManager().renameWindows();
        }));
        add(GUIUtil.getButtonWithMethod(new JButton("Launch All Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            SafeInstanceLauncher.launchInstances(julti.getInstanceManager().getInstances());
        }));
        add(GUIUtil.getButtonWithMethod(new JButton("Close All Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                instance.closeWindow();
            }
        }));
        add(GUIUtil.getButtonWithMethod(new JButton("Reset Instance Positions"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                instance.ensureWindowState();
            }
        }));
        add(GUIUtil.getButtonWithMethod(new JButton("Options..."), actionEvent -> {
            if (optionsGUI == null || optionsGUI.isClosed()) {
                optionsGUI = new OptionsGUI(julti, gui);
            } else {
                optionsGUI.requestFocus();
            }
        }));
    }

    public OptionsGUI getOptionsGUI() {
        return optionsGUI;
    }
}
