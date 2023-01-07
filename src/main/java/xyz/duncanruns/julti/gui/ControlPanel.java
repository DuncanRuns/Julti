package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatMarginBorder;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.SafeInstanceLauncher;
import xyz.duncanruns.julti.util.SyncUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class ControlPanel extends JPanel {
    private final Julti julti;
    private OptionsGUI optionsGUI = null;

    public ControlPanel(Julti julti, JultiGUI gui) {
        this.julti = julti;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.ipadx = 5;
        gbc2.ipady = 5;
        gbc2.fill = 2;
        gbc2.gridx = 0;
        GridBagConstraints gbc = (GridBagConstraints) gbc2.clone();
        gbc.insets = new Insets(2, 0, 2, 0);

        setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));

        add(GUIUtil.getButtonWithMethod(new JButton("Redetect Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            if (0 == JOptionPane.showConfirmDialog(gui, "This will remove all instances saved to the profile and replace them with new ones.\nAre you sure you want to do this?", "Julti: Redetect Instances", JOptionPane.WARNING_MESSAGE))
                julti.redetectInstances();
        }), gbc);

        add(GUIUtil.getButtonWithMethod(new JButton("Reset Instance Data"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            julti.resetInstanceData();
        }), gbc);
        add(GUIUtil.createSpacer(2), gbc2);

        add(GUIUtil.getButtonWithMethod(new JButton("Launch All Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            if (SafeInstanceLauncher.launchInstances(julti.getInstanceManager().getInstances())) {
                JultiGUI.log(Level.INFO, "Instances launched");
            } else {
                JultiGUI.log(Level.WARN, "Cannot launch instances! Is your MultiMC.exe path set?");
            }
        }), gbc);

        add(GUIUtil.getButtonWithMethod(new JButton("Close All Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                instance.closeWindow();
            }
        }), gbc);

        add(GUIUtil.createSpacer(2), gbc2);

        add(GUIUtil.getButtonWithMethod(new JButton("Set Titles"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            julti.getInstanceManager().renameWindows();
        }), gbc);

        add(GUIUtil.getButtonWithMethod(new JButton("Reset Instance Positions"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                instance.ensureWindowState();
            }
        }), gbc);

        add(GUIUtil.createSpacer(2), gbc2);

        add(GUIUtil.getButtonWithMethod(new JButton("Clear Worlds"), actionEvent -> julti.getInstanceManager().clearAllWorlds()), gbc);

        add(GUIUtil.getButtonWithMethod(new JButton("Sync Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();
            int ans = JOptionPane.showConfirmDialog(this, "Copy mods and config from " + instances.get(0) + " to all other instances?", "Julti: Sync Instances", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans == 0) {
                new Thread(() -> {
                    Thread.currentThread().setName("instance-sync");
                    try {
                        SyncUtil.sync(instances, instances.get(0), true, true);
                    } catch (IOException e) {
                        JultiGUI.log(Level.ERROR, "Failed to copy files:\n" + e.getMessage());
                    }
                }).start();
            }
        }), gbc);

        add(GUIUtil.createSpacer(2), gbc2);

        add(GUIUtil.getButtonWithMethod(new JButton("Options..."), actionEvent -> {
            if (optionsGUI == null || optionsGUI.isClosed()) {
                optionsGUI = new OptionsGUI(julti, gui);
            } else {
                optionsGUI.requestFocus();
            }
        }), gbc);
    }

    public OptionsGUI getOptionsGUI() {
        return optionsGUI;
    }
}
