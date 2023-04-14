package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatMarginBorder;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

import static xyz.duncanruns.julti.Julti.log;

public class ControlPanel extends JPanel {
    private OptionsGUI optionsGUI = null;
    private ScriptsGUI scriptsGUI = null;

    public ControlPanel() {
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
                    if (0 == JOptionPane.showConfirmDialog(JultiGUI.getInstance(), "This will remove all instances saved to the profile and replace them with new ones.\nAre you sure you want to do this?", "Julti: Redetect Instances", JOptionPane.OK_CANCEL_OPTION)) {
                        Julti.doLater(() -> InstanceManager.getManager().redetectInstances());

                    }
                }
            });

            GUIUtil.addMenuItem(menu, "Reload Instance Options", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    Julti.doLater(() -> InstanceManager.getManager().getInstances().forEach(MinecraftInstance::discoverInformation));

                }
            });

            GUIUtil.addMenuItem(menu, "Launch All Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    SafeInstanceLauncher.launchInstances(InstanceManager.getManager().getInstances());
                }
            });

            GUIUtil.addMenuItem(menu, "Close All Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    Julti.doLater(() -> DoAllFastUtil.doAllFast(MinecraftInstance::closeWindow));
                }
            });

            GUIUtil.addMenuItem(menu, "Set Titles", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    Julti.doLater(() -> InstanceManager.getManager().renameWindows());
                }
            });

            GUIUtil.addMenuItem(menu, "Reset Instance Positions", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    SleepBGUtil.disableLock();
                    Julti.doLater(() -> DoAllFastUtil.doAllFast(MinecraftInstance::ensureResettingWindowState));
                }
            });

            Point mousePos = this.getMousePosition();
            menu.show(this, mousePos.x, mousePos.y);
        }), gbc);

        // add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("File Utilities..."), a -> {
            JPopupMenu menu = new JPopupMenu("File Utilities");

            GUIUtil.addMenuItem(menu, "Clear Worlds", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BopperUtil.clearWorlds();
                }
            });

            GUIUtil.addMenuItem(menu, "Sync Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    List<MinecraftInstance> instances = InstanceManager.getManager().getInstances();
                    int ans = JOptionPane.showConfirmDialog(thisComponent, "Copy mods and config from " + instances.get(0) + " to all other instances?", "Julti: Sync Instances", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (ans == 0) {
                        new Thread(() -> {
                            try {
                                SyncUtil.sync(instances, instances.get(0), true, true);
                            } catch (IOException er) {
                                log(Level.ERROR, "Failed to copy files:\n" + er);
                            }
                        }, "instance-sync").start();
                    }
                }
            });

            Point mousePos = this.getMousePosition();
            menu.show(this, mousePos.x, mousePos.y);
        }), gbc);

        // add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Scripts..."), a -> {
            if (this.scriptsGUI == null || this.scriptsGUI.isClosed()) {
                this.scriptsGUI = new ScriptsGUI();
            } else {
                this.scriptsGUI.requestFocus();
            }
        }), gbc);

        // add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Options..."), a -> {
            if (this.optionsGUI == null || this.optionsGUI.isClosed()) {
                this.optionsGUI = new OptionsGUI();
            } else {
                this.optionsGUI.requestFocus();
            }
        }), gbc);
    }

    public OptionsGUI getOptionsGUI() {
        return this.optionsGUI;
    }
}
