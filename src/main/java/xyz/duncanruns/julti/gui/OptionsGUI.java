package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.ResetCounter;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.MonitorUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.Timer;
import java.util.*;

public class OptionsGUI extends JFrame {
    private static final String[] RESET_MODES = new String[]{"Multi", "Wall"};

    private boolean closed = false;
    private final Julti julti;
    private JTabbedPane tabbedPane;

    public OptionsGUI(Julti julti, JultiGUI gui) {
        this.julti = julti;
        setLocation(gui.getLocation());
        setupWindow();
        reloadComponents();
    }

    private static Component getWRCDField() {
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Long.class);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setValue(JultiOptions.getInstance().wallResetCooldown);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                JultiOptions.getInstance().wallResetCooldown = (long) field.getValue();
            }
        });
        GUIUtil.setActualSize(field, 50, 23);
        return field;
    }

    private void warnUnverifiable() {
        JultiOptions options = JultiOptions.getInstance();
        if (options.useJultiWallWindow && options.pauseRenderingDuringPlay && !options.wallResetAllAfterPlaying) {
            JOptionPane.showMessageDialog(this, "Warning: Any instances that are not being actively played will not be rendered and will be considered unverifiable, please enable \"Reset All After Playing\" OR disable \"Pause Rendering During Play\".", "Julti - Verification Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    private void setupWindow() {
        setLayout(null);
        setTitle("Julti Options");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        setSize(500, 350);
        setVisible(true);
    }

    private void reloadComponents() {
        getContentPane().removeAll();
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        setContentPane(tabbedPane);
        addComponentsProfile();
        addComponentsReset();
        addComponentsWall();
        addComponentsWindow();
        addComponentsHotkey();
        addComponentsOBS();
        addComponentsOther();
        revalidate();
        repaint();
    }

    private void addComponentsOther() {
        JPanel panel = createNewOptionsPanel("Other");

        panel.add(GUIUtil.leftJustify(new JLabel("Other Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        JButton counterButton = new JButton("Set Reset Counter (" + JultiOptions.getInstance().resetCounter + ")");
        counterButton.addActionListener(e -> {
            String ans = (String) JOptionPane.showInputDialog(this, "Please enter the amount of resets you want your counter to be at.", "Julti: Set Reset Counter", JOptionPane.QUESTION_MESSAGE, null, null, JultiOptions.getInstance().resetCounter);
            if (ans == null || ans.equals("")) return;
            ResetCounter.set(Integer.parseInt(ans));
            counterButton.setText("Set Reset Counter (" + JultiOptions.getInstance().resetCounter + ")");
        });
        panel.add(GUIUtil.leftJustify(counterButton));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("MultiMC Executable Path (.exe):")));
        panel.add(GUIUtil.createSpacer());

        JTextField mmcField = new JTextField(JultiOptions.getInstance().multiMCPath);
        GUIUtil.setActualSize(mmcField, 300, 23);
        mmcField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                JultiOptions.getInstance().multiMCPath = mmcField.getText();
            }
        });
        panel.add((GUIUtil.leftJustify(mmcField)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Auto-detect..."), actionEvent -> runMMCExecutableHelper(mmcField))));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Affinity", "useAffinity", b -> {
            reload();
            if (b) {
                AffinityManager.start(julti);
            } else {
                AffinityManager.stop();
                AffinityManager.release(julti);
            }
        })));

        if (!JultiOptions.getInstance().useAffinity) return;
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Affinity Threads:")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Currently Playing", "threadsPlaying")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Before Preview", "threadsPrePreview")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Start of Preview", "threadsStartPreview")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Rest of Preview", "threadsPreview")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("World Loaded", "threadsWorldLoaded")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Locked", "threadsLocked")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createThreadsSlider("Background", "threadsBackground")));
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
            if (names == null) continue;
            for (String name : names) {
                Path toCheck = possibleLocation.resolve(name);
                if (toCheck.toFile().isFile() && appNames.contains(name.toLowerCase())) {
                    candidates.add(toCheck);
                } else if (toCheck.toFile().exists() && toCheck.toFile().isDirectory()) {
                    String[] names2 = toCheck.toFile().list();
                    if (names2 == null) continue;
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
                browseForMMCExecutable(mmcField);
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
        if (ans == -1) return;
        if (ans == candidates.size()) {
            browseForMMCExecutable(mmcField);
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
        JPanel panel = createNewOptionsPanel("OBS");

        panel.add(GUIUtil.leftJustify(new JLabel("OBS Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        JButton dirtCoverButton = new JButton("Release Dirt Cover at " + JultiOptions.getInstance().dirtReleasePercent + "% Loaded");
        dirtCoverButton.addActionListener(e -> {
            String ans = (String) JOptionPane.showInputDialog(this, "Please enter the minimum percentage for removing dirt covers.", "Julti: Set Dirt Cover Release Percentage", JOptionPane.QUESTION_MESSAGE, null, null, JultiOptions.getInstance().dirtReleasePercent);
            if (ans == null || ans.equals("")) return;
            JultiOptions.getInstance().dirtReleasePercent = Integer.parseInt(ans);
            dirtCoverButton.setText("Release Dirt Cover at " + JultiOptions.getInstance().dirtReleasePercent + "% Loaded");
        });
        panel.add(GUIUtil.leftJustify(dirtCoverButton));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Projector Name Format:")));
        JTextField ownfField = new JTextField(JultiOptions.getInstance().obsWindowNameFormat);
        ownfField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                JultiOptions.getInstance().obsWindowNameFormat = ownfField.getText();
            }
        });
        GUIUtil.setActualSize(ownfField, 200, 23);
        panel.add(GUIUtil.leftJustify(ownfField));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Press Hotkeys", "obsPressHotkeys", aBoolean -> reload())));
        if (JultiOptions.getInstance().obsPressHotkeys) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Numpad", "obsUseNumpad")));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Alt", "obsUseAlt")));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("switchToWallHotkey", "Wall Scene Hotkey", julti, false)));
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("No settings here are required to use the OBS Link Script.")));
    }

    private void addComponentsHotkey() {
        JPanel panel = createNewOptionsPanel("Hotkeys");

        panel.add(GUIUtil.leftJustify(new JLabel("Hotkeys")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Right click to clear (disable) hotkey")));
        panel.add(GUIUtil.leftJustify(new JLabel("Checkboxes = Allow extra keys (ignore ctrl/shift/alt)")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("In-Game Hotkeys")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("resetHotkey", "Reset", julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("bgResetHotkey", "Background Reset", julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("fullscreenHotkey", "Go Fullscreen", julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Wall Hotkeys")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallResetHotkey", "Full Reset", julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallSingleResetHotkey", "Reset Instance", julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallLockHotkey", "Lock Instance", julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallPlayHotkey", "Play Instance", julti, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallFocusResetHotkey", "Focus Reset", julti, true)));

    }

    private JPanel createNewOptionsPanel(String name) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        tabbedPane.add(name, new JScrollPane(panel));
        return panel;
    }

    private void addComponentsProfile() {
        JPanel panel = createNewOptionsPanel("Profile");

        panel.add(GUIUtil.leftJustify(new JLabel("Profile")));
        panel.add(GUIUtil.createSpacer());

        JComboBox<String> profileSelectBox = new JComboBox<>(JultiOptions.getProfileNames());
        GUIUtil.setActualSize(profileSelectBox, 200, 22);
        profileSelectBox.addActionListener(e -> {
            julti.changeProfile(profileSelectBox.getSelectedItem().toString());
            reloadComponents();
        });

        panel.add(GUIUtil.leftJustify(profileSelectBox));
        panel.add(GUIUtil.createSpacer());

        OptionsGUI thisGUI = this;
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Remove"), actionEvent -> {
            String toRemove = JultiOptions.getSelectedProfileName();
            if (0 != JOptionPane.showConfirmDialog(thisGUI, "Are you sure you want to remove the profile \"" + toRemove + "\"?", "Julti: Remove Profile", JOptionPane.WARNING_MESSAGE))
                return;
            String switchTo = "";
            for (String name : JultiOptions.getProfileNames()) {
                if (!name.equals(toRemove)) {
                    switchTo = name;
                    break;
                }
            }
            julti.changeProfile(switchTo);
            JultiOptions.removeProfile(toRemove);
            reloadComponents();
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
                julti.changeProfile(newName);
                reloadComponents();
            }
        })));
    }

    private void addComponentsWall() {
        JPanel panel = createNewOptionsPanel("Wall");

        panel.add(GUIUtil.leftJustify(new JLabel("Wall Settings")));
        panel.add(GUIUtil.createSpacer());
        if (JultiOptions.getInstance().resetMode != 1) {
            panel.add(GUIUtil.leftJustify(new JLabel("Resetting mode is on Multi! A lot of these settings are only relevant to Wall mode.")));
            panel.add(GUIUtil.createSpacer());
        }
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Reset All After Playing", "wallResetAllAfterPlaying", b -> {
            warnUnverifiable();
            reload();
        })));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Don't Focus Unloaded Instances", "wallLockInsteadOfPlay")));


        if (!JultiOptions.getInstance().wallResetAllAfterPlaying) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Bypass Wall (Skip to next Instance)", "wallBypass", b -> reload())));
            if (JultiOptions.getInstance().wallBypass) {
                panel.add(GUIUtil.createSpacer());
                panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Return to Wall if None Loaded", "returnToWallIfNoneLoaded")));
            }
        }
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Reset Cooldown:")));
        panel.add(GUIUtil.leftJustify(getWRCDField()));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Automatically Determine Wall Layout", "autoCalcWallSize", b -> reload())));
        panel.add(GUIUtil.createSpacer());
        if (!JultiOptions.getInstance().autoCalcWallSize) {
            panel.add(GUIUtil.leftJustify(new WallSizeComponent()));
            panel.add(GUIUtil.createSpacer());
        }

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        addUJWWOption(panel);

        if (JultiOptions.getInstance().useJultiWallWindow) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Pause Rendering During Play", "pauseRenderingDuringPlay", b -> warnUnverifiable())));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Window Settings for Wall Window Size", "jwUseWindowSize")));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Show Lock Icons", "jwShowLockIcons")));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Darken Locked Instances (May lag)", "jwDarkenLocked")));
            panel.add(GUIUtil.createSpacer());
            JSlider darkenSlider = new JSlider(0, 0, 100, JultiOptions.getInstance().jwDarkenLevel);
            darkenSlider.addChangeListener(e -> JultiOptions.getInstance().jwDarkenLevel = darkenSlider.getValue());
            GUIUtil.setActualSize(darkenSlider, 200, 23);
            panel.add(GUIUtil.leftJustify(darkenSlider));
        }

    }

    private void addUJWWOption(JPanel panel) {
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Julti's Wall Window", "useJultiWallWindow", b -> {
            reload();
            julti.checkWallWindow();
            if (b) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        requestFocus();
                    }
                }, 100);
            }
            warnUnverifiable();
            requestFocus();
        })));
    }

    private void addComponentsReset() {
        JPanel panel = createNewOptionsPanel("Resetting");

        panel.add(GUIUtil.leftJustify(new JLabel("Reset Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Style:")));

        JComboBox<String> resetStyleBox = new JComboBox<>(RESET_MODES);
        resetStyleBox.setSelectedItem(RESET_MODES[JultiOptions.getInstance().resetMode]);
        resetStyleBox.addActionListener(e -> {
            JultiOptions.getInstance().resetMode = Arrays.asList(RESET_MODES).indexOf(resetStyleBox.getSelectedItem().toString());
            reload();
            julti.reloadManagers();
        });
        GUIUtil.setActualSize(resetStyleBox, 100, 23);

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
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Clipboard on Reset:")));

        JTextField corField = new JTextField(JultiOptions.getInstance().clipboardOnReset);
        corField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                JultiOptions.getInstance().clipboardOnReset = corField.getText();
            }
        });
        GUIUtil.setActualSize(corField, 200, 23);
        panel.add(GUIUtil.leftJustify(corField));
    }

    private void reload() {
        // Get current index
        int index = getTabbedPane().getSelectedIndex();
        // Get current scroll
        int s = ((JScrollPane) getTabbedPane().getSelectedComponent()).getVerticalScrollBar().getValue();
        // Reload
        reloadComponents();
        // Set index
        getTabbedPane().setSelectedIndex(index);
        // Set scroll
        ((JScrollPane) getTabbedPane().getSelectedComponent()).getVerticalScrollBar().setValue(s);

    }

    private void addComponentsWindow() {
        JPanel panel = createNewOptionsPanel("Window");

        panel.add(GUIUtil.leftJustify(new JLabel("Window Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Let Julti Manage Windows", "letJultiMoveWindows", b -> reload())));

        if (!JultiOptions.getInstance().letJultiMoveWindows) return;
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
            if (ans == -1) return;
            JultiOptions options = JultiOptions.getInstance();
            MonitorUtil.Monitor monitor = monitors[ans];
            options.windowPos = monitor.position;
            options.windowSize = monitor.size;
            windowOptions.reload();
            revalidate();
        })));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());


        final JLabel squishLabel = new JLabel("Wide Reset Squish Level: (" + JultiOptions.getInstance().wideResetSquish + ")");
        panel.add(GUIUtil.leftJustify(squishLabel));
        JSlider squishSlider = new JSlider(0, 10, 80, (int) (JultiOptions.getInstance().wideResetSquish * 10));
        squishSlider.addChangeListener(e -> {
            JultiOptions.getInstance().wideResetSquish = squishSlider.getValue() / 10f;
            squishLabel.setText("Wide Reset Squish Level: (" + JultiOptions.getInstance().wideResetSquish + ")");
        });
        GUIUtil.setActualSize(squishSlider, 200, 23);
        panel.add(GUIUtil.leftJustify(squishSlider));
    }

    public boolean isClosed() {
        return closed;
    }

    private void onClose() {
        closed = true;
        julti.reloadInstancePositions();
    }
}
