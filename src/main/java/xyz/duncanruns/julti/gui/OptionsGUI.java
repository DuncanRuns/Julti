package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.MonitorUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionsGUI extends JFrame {
    private static final String[] RESET_MODES = new String[]{"Multi", "Wall", "Dynamic Wall"};

    private boolean closed = false;
    private final Julti julti;
    private JTabbedPane tabbedPane;

    public OptionsGUI(Julti julti, JultiGUI gui) {
        this.julti = julti;
        this.setLocation(gui.getLocation());
        this.setupWindow();
        this.reloadComponents();
    }

    private JTabbedPane getTabbedPane() {
        return this.tabbedPane;
    }

    private void setupWindow() {
        this.setLayout(null);
        this.setTitle("Julti Options");
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                OptionsGUI.this.onClose();
            }
        });
        this.setSize(600, 400);
        this.setVisible(true);
    }

    private void reloadComponents() {
        this.getContentPane().removeAll();
        this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        this.tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.setContentPane(this.tabbedPane);
        this.addComponentsProfile();
        this.addComponentsReset();
        this.addComponentsWall();
        this.addComponentsWindow();
        this.addComponentsHotkey();
        this.addComponentsOBS();
        this.addComponentsSound();
        this.addComponentsAffinity();
        this.addComponentsOther();
        this.revalidate();
        this.repaint();
    }

    private void addComponentsSound() {
        JPanel panel = this.createNewOptionsPanel("Sound");

        panel.add(GUIUtil.leftJustify(new JLabel("Sound Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Right click selected sounds to clear")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());

        Path jultiDir = JultiOptions.getJultiDir().resolve("sounds");

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Single Reset Sound:")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "singleResetSound", "wav", jultiDir)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("singleResetVolume")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Multi Reset Sound:")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "multiResetSound", "wav", jultiDir)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("multiResetVolume")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Lock Sound:")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "lockSound", "wav", jultiDir)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("lockVolume")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Play Sound:")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "playSound", "wav", jultiDir)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("playVolume")));
    }

    private void addComponentsAffinity() {
        JPanel panel = this.createNewOptionsPanel("Affinity");

        JultiOptions options = JultiOptions.getInstance();

        panel.add(GUIUtil.leftJustify(new JLabel("Affinity Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Affinity", "useAffinity", b -> {
            this.reload();
            if (b) {
                AffinityManager.start(this.julti);
            } else {
                AffinityManager.stop();
                AffinityManager.release(this.julti);
            }
        })));

        if (!options.useAffinity) {
            return;
        }
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Affinity Threads:")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Currently Playing", "threadsPlaying")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Before Preview", "threadsPrePreview")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Start of Preview", "threadsStartPreview")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Rest of Preview", "threadsPreview")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("World Loaded", "threadsWorldLoaded")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Locked", "threadsLocked")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Background", "threadsBackground")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("affinityBurst", "Affinity Burst", this, "ms")));
    }

    private void addComponentsOther() {
        JPanel panel = this.createNewOptionsPanel("Other");

        JultiOptions options = JultiOptions.getInstance();

        panel.add(GUIUtil.leftJustify(new JLabel("Other Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("resetCounter", "Reset Counter", this)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("MultiMC Executable Path (.exe):")));
        panel.add(GUIUtil.createSpacer());

        JTextField mmcField = new JTextField(options.multiMCPath);
        GUIUtil.setActualSize(mmcField, 300, 23);
        mmcField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                this.update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                this.update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                this.update();
            }

            private void update() {
                options.multiMCPath = mmcField.getText();
            }
        });
        panel.add((GUIUtil.leftJustify(mmcField)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Auto-detect..."), actionEvent -> this.runMMCExecutableHelper(mmcField))));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Launch Instances Offline", "launchOffline", b -> this.reload())));
        panel.add(GUIUtil.createSpacer());

        if (options.launchOffline) {
            panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("launchOfflineName", "Offline Name", this)));
            panel.add(GUIUtil.createSpacer());
        }
    }

    private void runMMCExecutableHelper(JTextField mmcField) {
        List<String> appNames = Arrays.asList("multimc.exe,prismlauncher.exe".split(","));
        List<Path> possibleLocations = new ArrayList<>();
        Path userHome = Paths.get(System.getProperty("user.home"));
        possibleLocations.add(userHome.resolve("Desktop"));
        possibleLocations.add(userHome.resolve("Documents"));
        possibleLocations.add(userHome.resolve("AppData").resolve("Roaming"));
        possibleLocations.add(userHome.resolve("AppData").resolve("Local").resolve("Programs"));
        possibleLocations.add(userHome.resolve("Downloads"));
        possibleLocations.add(Paths.get("C:\\"));

        List<Path> candidates = new ArrayList<>();
        for (Path possibleLocation : possibleLocations) {
            String[] names = possibleLocation.toFile().list();
            if (names == null) {
                continue;
            }
            for (String name : names) {
                Path toCheck = possibleLocation.resolve(name);
                if (toCheck.toFile().isFile() && appNames.contains(name.toLowerCase())) {
                    candidates.add(toCheck);
                } else if (toCheck.toFile().exists() && toCheck.toFile().isDirectory()) {
                    String[] names2 = toCheck.toFile().list();
                    if (names2 == null) {
                        continue;
                    }
                    for (String name2 : names2) {
                        Path toCheck2 = toCheck.resolve(name2);
                        if (toCheck2.toFile().isFile() && appNames.contains(name2.toLowerCase())) {
                            candidates.add(toCheck2);
                        }
                    }
                }
            }
        }
        if (candidates.size() == 0) {
            if (0 == JOptionPane.showConfirmDialog(this, "Could not automatically find any candidates, browse for exe instead?", "Julti: Choose MultiMC Executable", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                this.browseForMMCExecutable(mmcField);
            }
            return;
        }

        Object[] options = new Object[candidates.size() + 1];
        int i = 0;
        StringBuilder message = new StringBuilder("Please choose one of the following, or browse:");
        for (Path candidate : candidates) {
            options[i++] = i + " - " + candidate.getName(candidate.getNameCount() - 1);
            message.append("\n").append(i).append(" - ").append(candidate);
        }
        options[candidates.size()] = "Browse...";
        // The ans int will be the index of the candidate, or one larger than any possible index to indicate browsing.
        int ans = JOptionPane.showOptionDialog(this, message.toString(), "Julti: Choose MultiMC Executable", JOptionPane.CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
        if (ans == -1) {
            return;
        }
        if (ans == candidates.size()) {
            this.browseForMMCExecutable(mmcField);
        } else {
            Path chosen = candidates.get(ans);
            JultiOptions.getInstance().multiMCPath = chosen.toString();
            mmcField.setText(chosen.toString());
        }
    }

    private void browseForMMCExecutable(JTextField mmcField) {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setDialogTitle("Julti: Choose MultiMC Executable");
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("Executables", "exe"));

        int val = jfc.showOpenDialog(this);
        if (val == JFileChooser.APPROVE_OPTION) {

            String chosen = jfc.getSelectedFile().toPath().toString();
            JultiOptions.getInstance().multiMCPath = chosen;
            mmcField.setText(chosen);
        }
    }

    private void addComponentsOBS() {
        JPanel panel = this.createNewOptionsPanel("OBS");

        panel.add(GUIUtil.leftJustify(new JLabel("OBS Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("obsWindowNameFormat", "Projector Name Format", this)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("instanceSpacing", "Instance Spacing (Border)", this)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Press Hotkeys", "obsPressHotkeys", aBoolean -> this.reload())));
        if (JultiOptions.getInstance().obsPressHotkeys) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Numpad", "obsUseNumpad")));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Alt", "obsUseAlt")));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("switchToWallHotkey", "Wall Scene Hotkey", this.julti, false)));
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("No settings here are required to use the OBS Link Script.")));
    }

    private void addComponentsHotkey() {
        JultiOptions options = JultiOptions.getInstance();

        JPanel panel = this.createNewOptionsPanel("Hotkeys");

        panel.add(GUIUtil.leftJustify(new JLabel("Hotkeys")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Right click to clear (disable) hotkey")));
        panel.add(GUIUtil.leftJustify(new JLabel("Checkboxes = Allow extra keys (ignore ctrl/shift/alt)")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("In-Game Hotkeys")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("resetHotkey", "Reset", this.julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("bgResetHotkey", "Background Reset", this.julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Wall Hotkeys")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallResetHotkey", "Full Reset", this.julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallSingleResetHotkey", "Reset Instance", this.julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallLockHotkey", "Lock Instance", this.julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallPlayHotkey", "Play Instance", this.julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallFocusResetHotkey", "Focus Reset", this.julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallPlayLockHotkey", "Play Next Lock", this.julti, true)));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Script Hotkeys")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("cancelScriptHotkey", "Cancel Running Script", this.julti, true)));

        for (String scriptName : ScriptManager.getHotkeyableScriptNames()) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createScriptHotkeyChangeButton(scriptName, this.julti, this::reload)));
        }
    }

    private JPanel createNewOptionsPanel(String name) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        this.tabbedPane.add(name, scrollPane);
        return panel;
    }

    private void addComponentsProfile() {
        JPanel panel = this.createNewOptionsPanel("Profile");

        panel.add(GUIUtil.leftJustify(new JLabel("Profile")));
        panel.add(GUIUtil.createSpacer());

        JComboBox<String> profileSelectBox = new JComboBox<>(JultiOptions.getProfileNames());
        GUIUtil.setActualSize(profileSelectBox, 200, 22);
        profileSelectBox.addActionListener(e -> {
            this.julti.changeProfile(profileSelectBox.getSelectedItem().toString());
            this.reloadComponents();
        });

        panel.add(GUIUtil.leftJustify(profileSelectBox));
        panel.add(GUIUtil.createSpacer());

        OptionsGUI thisGUI = this;
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Remove"), actionEvent -> {
            String toRemove = JultiOptions.getSelectedProfileName();
            if (0 != JOptionPane.showConfirmDialog(thisGUI, "Are you sure you want to remove the profile \"" + toRemove + "\"?", "Julti: Remove Profile", JOptionPane.WARNING_MESSAGE)) {
                return;
            }
            String switchTo = "";
            for (String name : JultiOptions.getProfileNames()) {
                if (!name.equals(toRemove)) {
                    switchTo = name;
                    break;
                }
            }
            this.julti.changeProfile(switchTo);
            JultiOptions.removeProfile(toRemove);
            this.reloadComponents();
        })));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("New"), actionEvent -> {
            String newName = JOptionPane.showInputDialog(thisGUI, "Enter a new profile name:", "Julti: New Profile", JOptionPane.QUESTION_MESSAGE);
            if (newName != null) {
                if (Arrays.asList(JultiOptions.getProfileNames()).contains(newName)) {
                    JOptionPane.showMessageDialog(thisGUI, "Profile already exists!", "Julti: Cannot Create New Profile", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JultiOptions.getInstance().copyTo(newName);
                this.julti.changeProfile(newName);
                this.reloadComponents();
            }
        })));
    }

    private void addComponentsWall() {
        JPanel panel = this.createNewOptionsPanel("Wall");

        JultiOptions options = JultiOptions.getInstance();

        panel.add(GUIUtil.leftJustify(new JLabel("Wall Settings")));
        panel.add(GUIUtil.createSpacer());
        if (options.resetMode == 0) {
            panel.add(GUIUtil.leftJustify(new JLabel("Resetting mode is on Multi! A lot of these settings are only relevant to Wall mode.")));
            panel.add(GUIUtil.createSpacer());
        }
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Clean Wall (F1 on world load)", "cleanWall")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Reset All After Playing", "wallResetAllAfterPlaying", b -> {
            this.reload();
        })));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Don't Focus Unloaded Instances", "wallLockInsteadOfPlay")));


        if (!options.wallResetAllAfterPlaying) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Bypass Wall (Skip to next Instance)", "wallBypass", b -> this.reload())));
            if (options.wallBypass) {
                panel.add(GUIUtil.createSpacer());
                panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Return to Wall if None Loaded", "returnToWallIfNoneLoaded")));
            }
        }
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Automatically Determine Wall Layout", "autoCalcWallSize", b -> this.reload())));
        if (!options.autoCalcWallSize) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(new WallSizeComponent()));
        }

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBox("Use Dirt Covers", options.dirtReleasePercent >= 0, b -> {
            options.dirtReleasePercent = b ? 0 : -1;
            this.reload();
        })));
        if (options.dirtReleasePercent >= 0) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("dirtReleasePercent", "Dirt Cover Release Percent", this, "%")));
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("wallResetCooldown", "Reset Cooldown", this)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("The reset cooldown starts as soon as you can see the instance,")));
        panel.add(GUIUtil.leftJustify(new JLabel("this also accounts for appearance based on dirt covers and dynamic wall.")));


        if (!(options.resetMode == 2)) {
            return;
        }
        // Dynamic wall settings below

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Dynamic Wall Settings")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Replace Locked Instances", "dwReplaceLocked")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("lockedInstanceSpace", "Locked Instance Space", this, "%")));
    }

    private void addComponentsReset() {
        JPanel panel = this.createNewOptionsPanel("Resetting");

        JultiOptions options = JultiOptions.getInstance();

        panel.add(GUIUtil.leftJustify(new JLabel("Reset Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Style:")));

        JComboBox<String> resetStyleBox = new JComboBox<>(RESET_MODES);
        resetStyleBox.setSelectedItem(RESET_MODES[options.resetMode]);
        resetStyleBox.addActionListener(e -> {
            options.resetMode = Arrays.asList(RESET_MODES).indexOf(resetStyleBox.getSelectedItem().toString());
            this.reload();
            this.julti.reloadManagers();
        });
        GUIUtil.setActualSize(resetStyleBox, 120, 23);

        panel.add(GUIUtil.leftJustify(resetStyleBox));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Pause On Load", "pauseOnLoad")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use F3", "useF3")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Unpause on Switch", "unpauseOnSwitch")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Coop Mode", "coopMode")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Pie Chart On Load (Illegal for normal runs)", "pieChartOnLoad")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("clipboardOnReset", "Clipboard On Reset", this)));
    }

    private void reload() {
        // Get current index
        int index = this.getTabbedPane().getSelectedIndex();
        // Get current scroll
        int s = ((JScrollPane) this.getTabbedPane().getSelectedComponent()).getVerticalScrollBar().getValue();
        // Reload
        this.reloadComponents();
        // Set index
        this.getTabbedPane().setSelectedIndex(index);
        // Set scroll
        ((JScrollPane) this.getTabbedPane().getSelectedComponent()).getVerticalScrollBar().setValue(s);

    }

    private void addComponentsWindow() {
        JPanel panel = this.createNewOptionsPanel("Window");

        JultiOptions options = JultiOptions.getInstance();

        panel.add(GUIUtil.leftJustify(new JLabel("Window Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Let Julti Manage Windows", "letJultiMoveWindows", b -> this.reload())));

        if (!options.letJultiMoveWindows) {
            return;
        }
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Borderless", "useBorderless")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        WindowOptionComponent windowOptions = new WindowOptionComponent();
        panel.add(windowOptions);
        panel.add(GUIUtil.createSpacer());

        OptionsGUI thisGUI = this;
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Choose Monitor"), actionEvent -> {
            MonitorUtil.Monitor[] monitors = MonitorUtil.getAllMonitors();
            StringBuilder monitorOptionsText = new StringBuilder();
            Object[] buttons = new Object[monitors.length];
            int i = 0;
            for (MonitorUtil.Monitor monitor : monitors) {
                buttons[i] = String.valueOf(i + 1);
                monitorOptionsText.append("\n#").append(++i).append(" - ").append("Size: ").append(monitor.width).append("x").append(monitor.height).append(", Position: (").append(monitor.x).append(",").append(monitor.y).append(")");
            }

            int ans = JOptionPane.showOptionDialog(thisGUI, "Choose a monitor:\n" + monitorOptionsText.toString().trim(), "Julti: Choose Monitor", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttons, null);
            if (ans == -1) {
                return;
            }
            MonitorUtil.Monitor monitor = monitors[ans];
            options.windowPos = monitor.position;
            options.windowSize = monitor.size;
            windowOptions.reload();
            this.revalidate();
        })));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());


        final JLabel squishLabel = new JLabel("Wide Reset Squish Level: (" + options.wideResetSquish + ")");
        panel.add(GUIUtil.leftJustify(squishLabel));
        JSlider squishSlider = new JSlider(0, 10, 80, (int) (options.wideResetSquish * 10));
        squishSlider.addChangeListener(e -> {
            options.wideResetSquish = squishSlider.getValue() / 10f;
            squishLabel.setText("Wide Reset Squish Level: (" + options.wideResetSquish + ")");
        });
        GUIUtil.setActualSize(squishSlider, 200, 23);
        panel.add(GUIUtil.leftJustify(squishSlider));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Unsquish When Locking", "unsquishOnLock")));
    }

    public boolean isClosed() {
        return this.closed;
    }

    private void onClose() {
        this.closed = true;
        this.julti.reloadInstancePositions();
        this.julti.tryOutputLSInfo();
    }
}
