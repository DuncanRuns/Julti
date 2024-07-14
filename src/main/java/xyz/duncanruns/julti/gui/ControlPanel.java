package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatMarginBorder;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static xyz.duncanruns.julti.Julti.log;

public class ControlPanel extends JPanel {
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

            if (!JultiOptions.getJultiOptions().utilityMode) {
                GUIUtil.addMenuItem(menu, "Redetect Instances", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Thread.currentThread().setName("julti-gui");
                        if (0 == JOptionPane.showConfirmDialog(JultiGUI.getJultiGUI(), "This will remove all instances saved to the profile and replace them with new ones.\nAre you sure you want to do this?", "Julti: Redetect Instances", JOptionPane.OK_CANCEL_OPTION)) {
                            Julti.doLater(() -> InstanceManager.getInstanceManager().redetectInstances());
                        }
                    }
                });
            }

            GUIUtil.addMenuItem(menu, "Reload Instance Options", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    Julti.doLater(() -> InstanceManager.getInstanceManager().getInstances().forEach(MinecraftInstance::discoverInformation));

                }
            });
            if (!JultiOptions.getJultiOptions().utilityMode) {
                GUIUtil.addMenuItem(menu, "Launch All Instances", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Thread.currentThread().setName("julti-gui");
                        SafeInstanceLauncher.launchInstances(InstanceManager.getInstanceManager().getInstances());
                    }
                });
            }

            GUIUtil.addMenuItem(menu, "Close All Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    if (0 == JOptionPane.showConfirmDialog(JultiGUI.getJultiGUI(), "Are you sure you'd like to close all of your instances?", "Julti: Close Instances", JOptionPane.OK_CANCEL_OPTION)) {
                        Julti.doLater(() -> DoAllFastUtil.doAllFast(MinecraftInstance::closeWindow));
                    }
                }
            });

            GUIUtil.addMenuItem(menu, "Set Titles", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    Julti.doLater(() -> InstanceManager.getInstanceManager().renameWindows());
                }
            });

            GUIUtil.addMenuItem(menu, "Reset Instance Positions", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    SleepBGUtil.disableLock();
                    Julti.doLater(Julti::resetInstancePositions);
                }
            });

            Point mousePos = this.getMousePosition();
            if (mousePos == null) {
                mousePos = new Point(0, 0);
            }
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

            GUIUtil.addMenuItem(menu, "Launch Programs", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LauncherUtil.launchPrograms();
                }
            });

            GUIUtil.addMenuItem(menu, "Sync Instances", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    if (InstanceManager.getInstanceManager().getInstances().size() < 2) {
                        Julti.log(Level.ERROR, "Can't sync instances with less than 2 instances!");
                        return;
                    }
                    Optional<Set<SyncUtil.SyncOptions>> ans = SyncUtil.ask();
                    if (!ans.isPresent()) {
                        return;
                    }
                    new Thread(() -> {
                        try {
                            List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
                            SyncUtil.sync(instances, instances.get(0), ans.get());
                        } catch (IOException er) {
                            log(Level.ERROR, "Failed to copy files:\n" + ExceptionUtil.toDetailedString(er));
                        }
                    }, "instance-sync").start();

                }
            });

            GUIUtil.addMenuItem(menu, "Open .Julti", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Desktop.getDesktop().browse(JultiOptions.getJultiDir().toAbsolutePath().toUri());
                    } catch (IOException ex) {
                        Julti.log(Level.ERROR, "Failed to open .Julti folder:\n" + ExceptionUtil.toDetailedString(ex));
                    }
                }
            });

            Point mousePos = this.getMousePosition();
            if (mousePos == null) {
                mousePos = new Point(0, 0);
            }
            menu.show(this, mousePos.x, mousePos.y);
        }), gbc);

        // add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Scripts..."), a -> ScriptsGUI.openGUI()), gbc);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Plugins..."), a -> PluginsGUI.openGUI()), gbc);

        // add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Options..."), a -> OptionsGUI.openGUI()), gbc);

        if (!Objects.equals("DEV", Julti.VERSION)) {
            return;
        }

        this.add(GUIUtil.createSpacer(2), gbc2);

        this.add(GUIUtil.getButtonWithMethod(new JButton("Dev Utilities..."), a -> {
            JPopupMenu menu = new JPopupMenu("Dev Utilities");

            GUIUtil.addMenuItem(menu, "Do waitForExecute Crash", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Julti.waitForExecute(() -> {
                        throw new RuntimeException("Test Crash!!!");
                    });
                }
            });

            GUIUtil.addMenuItem(menu, "Do GUI Crash", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new RuntimeException("Test Crash!!!");
                }
            });

            Point mousePos = this.getMousePosition();
            if (mousePos == null) {
                mousePos = new Point(0, 0);
            }
            menu.show(this, mousePos.x, mousePos.y);
        }), gbc);
    }

    @Deprecated
    public ScriptsGUI openScriptsGUI() {
        return ScriptsGUI.openGUI();
    }

    @Deprecated
    public ScriptsGUI getScriptsGUI() {
        return ScriptsGUI.getGUI();
    }

    @Deprecated
    public PluginsGUI openPluginsGui() {
        return PluginsGUI.openGUI();
    }

    @Deprecated
    public PluginsGUI getPluginsGUI() {
        return PluginsGUI.getGUI();
    }

    @Deprecated
    public OptionsGUI openOptions() {
        return OptionsGUI.getGUI();
    }

    @Deprecated
    public OptionsGUI getOptionsGUI() {
        return OptionsGUI.openGUI();
    }
}
