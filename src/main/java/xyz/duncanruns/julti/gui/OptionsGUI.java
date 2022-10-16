package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.MonitorUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionsGUI extends JFrame {
    private boolean closed = false;
    private final Julti julti;
    private JTabbedPane tabbedPane;

    public OptionsGUI(Julti julti, JultiGUI gui) {
        this.julti = julti;
        setLocation(gui.getLocation());
        setupWindow();
        reloadComponents();
    }

    private static Component createSpacerBox() {
        return Box.createRigidArea(new Dimension(0, 5));
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
        setSize(400, 300);
        setVisible(true);
    }

    private void reloadComponents() {
        getContentPane().removeAll();
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        setContentPane(tabbedPane);
        addComponentsProfile();
        addComponentsReset();
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
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Automatically Clear Worlds", "autoClearWorlds")));
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(new JLabel("MultiMC/PolyMC Executable Path:")));

        JTextField mmcField = new JTextField(JultiOptions.getInstance().multiMCPath, 20);
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
        panel.add(GUIUtil.leftJustify(mmcField));

        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Auto-detect..."), actionEvent -> runMMCExecutableHelper(mmcField))));
    }

    private void runMMCExecutableHelper(JTextField mmcField) {
        List<String> appNames = Arrays.asList("polymc.exe,multimc.exe".split(","));
        List<Path> possibleLocations = new ArrayList<>();
        Path userHome = Paths.get(System.getProperty("user.home"));
        possibleLocations.add(userHome.resolve("Desktop"));
        possibleLocations.add(userHome.resolve("Documents"));
        possibleLocations.add(userHome.resolve("AppData").resolve("Roaming"));
        possibleLocations.add(userHome.resolve("AppData").resolve("Local").resolve("Programs"));

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
            if (0 == JOptionPane.showConfirmDialog(this, "Could not automatically find any candidates, browse for exe instead?", "Julti: Choose MultiMC/PolyMC Executable", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                browseForMMCExecutable(mmcField);
            }
            return;
        }

        Object[] options = new Object[candidates.size() + 1];
        int i = 0;
        StringBuilder message = new StringBuilder("Please choose one of the following, or browse:");
        for (Path candidate : candidates) {
            options[i++] = i + " - " + candidate.getName(candidate.getNameCount() - 1).toString();
            message.append("\n").append(i).append(" - ").append(candidate);
        }
        options[candidates.size()] = "Browse...";
        // The ans int will be the index of the candidate, or one larger than any possible index to indicate browsing.
        int ans = JOptionPane.showOptionDialog(this, message.toString(), "Julti: Choose MultiMC/PolyMC Executable", JOptionPane.CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
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
        jfc.setDialogTitle("Julti: Choose MultiMC/PolyMC Executable");
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
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Press Hotkeys", "obsPressHotkeys")));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Numpad", "obsUseNumpad")));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Alt", "obsUseAlt")));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("switchToWallHotkey", "Wall Scene Hotkey", julti)));
    }

    private void addComponentsHotkey() {
        JPanel panel = createNewOptionsPanel("Hotkeys");

        panel.add(GUIUtil.leftJustify(new JLabel("Hotkeys")));
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("resetHotkey", "Reset", julti)));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("bgResetHotkey", "Background Reset", julti)));
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(new JLabel("Wall Hotkeys")));
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallResetHotkey", "Full Reset", julti)));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallSingleResetHotkey", "Reset Instance", julti)));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallLockHotkey", "Lock Instance", julti)));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallPlayHotkey", "Play Instance", julti)));

    }

    private JPanel createNewOptionsPanel(String name) {
        JPanel panel = new JPanel();
        JPanel outerPanel = new JPanel();
        outerPanel.setBorder(new FlatBorder());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        outerPanel.add(panel);
        tabbedPane.add(name, outerPanel);
        return panel;
    }

    private void addComponentsProfile() {
        JPanel panel = createNewOptionsPanel("Profile");

        panel.add(GUIUtil.leftJustify(new JLabel("Profile")));
        panel.add(createSpacerBox());

        JComboBox<String> profileSelectBox = new JComboBox<>(JultiOptions.getProfileNames());
        profileSelectBox.addActionListener(e -> {
            julti.changeProfile(profileSelectBox.getSelectedItem().toString());
            reloadComponents();
        });

        panel.add(GUIUtil.leftJustify(profileSelectBox));
        panel.add(createSpacerBox());

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

        panel.add(createSpacerBox());
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

    private void addComponentsReset() {
        JPanel panel = createNewOptionsPanel("Resetting");

        panel.add(GUIUtil.leftJustify(new JLabel("Reset Settings")));
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Pause On Load", "pauseOnLoad")));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use F3", "useF3")));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Wall", "useWall")));
        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Wall One At A Time", "wallOneAtATime")));

        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(new JLabel("Clipboard on Reset:")));

        JTextField corField = new JTextField(JultiOptions.getInstance().clipboardOnReset, 20);
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

        panel.add(corField);
    }

    private void addComponentsWindow() {
        JPanel panel = createNewOptionsPanel("Window");

        panel.add(GUIUtil.leftJustify(new JLabel("Window Settings")));
        panel.add(createSpacerBox());


        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Borderless", "useBorderless")));
        panel.add(createSpacerBox());

        WindowOptionComponent windowOptions = new WindowOptionComponent();
        panel.add(windowOptions);
        panel.add(createSpacerBox());

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

            int ans = JOptionPane.showOptionDialog(thisGUI, "Choose a monitor:\n" + monitorOptionsText.toString().trim(), "Julti: Choose Monitor", -1, JOptionPane.QUESTION_MESSAGE, null, buttons, null);
            if (ans == -1) return;
            JultiOptions options = JultiOptions.getInstance();
            MonitorUtil.Monitor monitor = monitors[ans];
            options.windowPos = monitor.position;
            options.windowSize = monitor.size;
            windowOptions.reload();
            revalidate();
        })));
    }

    public boolean isClosed() {
        return closed;
    }

    private void onClose() {
        closed = true;
        julti.reloadInstancePositions();
    }
}
