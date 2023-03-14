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
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

public class ControlPanel extends JPanel {
    private OptionsGUI optionsGUI = null;
    private ScriptsGUI scriptsGUI = null;

    public ControlPanel(Julti julti, JultiGUI gui) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.ipadx = 5;
        gbc2.ipady = 5;
        gbc2.fill = 2;
        gbc2.gridx = 0;
        GridBagConstraints gbc = (GridBagConstraints) gbc2.clone();
        gbc.insets = new Insets(2, 0, 2, 0);

        Component thisComponent = this;

        this.setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));
        this.add(GUIUtil.getButtonWithMethod(new JButton("Instance Utilities..."), a -> {
            JPopupMenu menu = new JPopupMenu("Instance Utilities");

            GUIUtil.addMenuItem(menu, "Redetect Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    if (0 == JOptionPane.showConfirmDialog(gui, "This will remove all instances saved to the profile and replace them with new ones.\nAre you sure you want to do this?", "Julti: Redetect Instances", JOptionPane.OK_CANCEL_OPTION))
                        julti.redetectInstances();
                }
            });

            GUIUtil.addMenuItem(menu, "Reset Instance Data", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    julti.resetInstanceData();
                }
            });

            GUIUtil.addMenuItem(menu, "Launch All Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    SafeInstanceLauncher.launchInstances(julti.getInstanceManager().getInstances());
                }
            });

            GUIUtil.addMenuItem(menu, "Close All Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                        instance.closeWindow();
                    }
                }
            });

            GUIUtil.addMenuItem(menu, "Set Titles", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    julti.getInstanceManager().renameWindows();
                }
            });

            GUIUtil.addMenuItem(menu, "Reset Instance Positions", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                        new Thread(() -> instance.ensureWindowState(true, false), "julti-gui").start();
                    }
                }
            });

            Point mousePos = this.getMousePosition();
            menu.show(this, mousePos.x, mousePos.y);
        }), gbc);

        // this.add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("File Utilities..."), a -> {
            JPopupMenu menu = new JPopupMenu("File Utilities");

            GUIUtil.addMenuItem(menu, "Clear Worlds", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    julti.getInstanceManager().clearAllWorlds();
                }
            });

            GUIUtil.addMenuItem(menu, "Sync Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();
                    int ans = JOptionPane.showConfirmDialog(thisComponent, "Copy mods and config from " + instances.get(0) + " to all other instances?", "Julti: Sync Instances", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (ans == 0) {
                        new Thread(() -> {
                            try {
                                SyncUtil.sync(instances, instances.get(0), true, true);
                            } catch (IOException er) {
                                JultiGUI.log(Level.ERROR, "Failed to copy files:\n" + er);
                            }
                        }, "instance-sync").start();
                    }
                }
            });

            Point mousePos = this.getMousePosition();
            menu.show(this, mousePos.x, mousePos.y);
        }), gbc);

        // this.add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Scripts..."), a -> {
            if (this.scriptsGUI == null || this.scriptsGUI.isClosed()) {
                this.scriptsGUI = new ScriptsGUI(julti, gui);
            } else {
                this.scriptsGUI.requestFocus();
            }
        }), gbc);

        // this.add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Options..."), a -> {
            if (this.optionsGUI == null || this.optionsGUI.isClosed()) {
                this.optionsGUI = new OptionsGUI(julti, gui);
            } else {
                this.optionsGUI.requestFocus();
            }
        }), gbc);
    }

    public OptionsGUI getOptionsGUI() {
        return this.optionsGUI;
    }
}
