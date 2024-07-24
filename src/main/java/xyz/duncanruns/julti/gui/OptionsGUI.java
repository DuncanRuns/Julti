package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.management.OBSStateManager;
import xyz.duncanruns.julti.resetting.DynamicWallResetManager;
import xyz.duncanruns.julti.resetting.MultiResetManager;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.*;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionsGUI extends JFrame {
    private static OptionsGUI instance = null;
    private boolean closed = false;
    private JTabbedPane tabbedPane;
    private final List<String> tabNames = new ArrayList<>();

    public OptionsGUI() {
        this.setupWindow();
        this.reloadComponents();
    }

    @Nullable
    public static OptionsGUI getGUI() {
        return instance;
    }

    public static OptionsGUI openGUI() {
        if (instance == null || instance.isClosed()) {
            instance = new OptionsGUI();
        }
        instance.setExtendedState(NORMAL);
        instance.requestFocus();
        return instance;
    }

    private static void changeProfile(String profile) {
        Julti.waitForExecute(() -> Julti.getJulti().changeProfile(profile));
    }

    public static void reloadIfOpen() {
        if (instance != null && !instance.isClosed()) {
            instance.reload();
        }
    }

    private JTabbedPane getTabbedPane() {
        return this.tabbedPane;
    }

    private void setupWindow() {
        Point location = JultiGUI.getJultiGUI().getLocation();
        this.setLocation(location.x, location.y + 30);
        this.setLayout(null);
        this.setTitle("Julti Options");
        this.setIconImage(JultiGUI.getLogo());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                OptionsGUI.this.onClose();
            }
        });
        this.setSize(750, 400);
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
        this.addComponentsLaunching();
        this.addComponentsOther();
        if (JultiOptions.getJultiOptions().enableExperimentalOptions) {
            this.addComponentsExperimental();
        }
        this.revalidate();
        this.repaint();
    }

    private void addComponentsExperimental() {
        JPanel panel = this.createNewOptionsPanel("Experimental");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Experimental Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Utility Mode", "utilityMode", b -> this.reload())));

        if (options.utilityMode) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Allow Resets In Utility", "utilityModeAllowResets")));
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Auto use playing size in Utility","Enables fullscreen/borderless and uses playing size", "utilityModeUsePlaying")));
        }

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Auto Fullscreen", "autoFullscreen", b -> this.reload())));

        if (options.autoFullscreen) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Fullscreen Before Unpause", "May reduce cursor issues, especially for thin BT users.", "fullscreenBeforeUnpause")));

            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Playing Size w/ Fullscreen", "If turned off, the Playing Window Size is ignored.", "usePlayingSizeWithFullscreen", b -> this.reload())));

            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("fullscreenDelay", "Added Fullscreen Delay", this, "ms")));
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Show Debug Messages", "showDebug")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Pie Chart On Load (Illegal for normal runs)", "pieChartOnLoad")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Prevent Window Naming", "preventWindowNaming")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Always On Top Projector", "alwaysOnTopProjector")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Minimize Projector When Playing", "minimizeProjectorWhenPlaying")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Activate Projector On Reset", "Recommended for use with Bypass Wall in conjunction with thin BT", "activateProjectorOnReset")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Alt Switching", "Presses LAlt when switching windows - recommended for those with LAlt unbound", "useAltSwitching")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Allow Reset During Generating", "allowResetDuringGenerating")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Resizeable Borderless", "Allows the window to be resized, restored and maximized when Use Borderless is checked.", "resizeableBorderless", b -> this.reload())));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Freeze Filter", "Freezes the instance on the OBS preview - workaround for \"sky bug\"", "useFreezeFilter", b -> this.reload())));
        if (options.useFreezeFilter) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("freezePercent", "Freeze Filter Activation Percent", this, "%")));
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("This option requires the OBS Freeze Filter plugin.")));
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
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "singleResetSound", "wav", jultiDir, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("singleResetVolume")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Multi Reset Sound:")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "multiResetSound", "wav", jultiDir, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("multiResetVolume")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Lock Sound:")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "lockSound", "wav", jultiDir, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("lockVolume")));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Play Sound:")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createFileSelectButton(panel, "playSound", "wav", jultiDir, true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createVolumeSlider("playVolume")));
    }

    private void addComponentsAffinity() {
        JPanel panel = this.createNewOptionsPanel("Affinity");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Affinity Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Affinity", "useAffinity", b -> {
            this.reload();
            if (!b) {
                AffinityManager.release();
            }
        })));

        if (!options.useAffinity) {
            return;
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Set Defaults"), e -> {
            JultiOptions defaults = JultiOptions.getDefaults();
            options.threadsPlaying = defaults.threadsPlaying;
            options.threadsPrePreview = defaults.threadsPrePreview;
            options.threadsStartPreview = defaults.threadsStartPreview;
            options.threadsPreview = defaults.threadsPreview;
            options.threadsWorldLoaded = defaults.threadsWorldLoaded;
            options.threadsLocked = defaults.threadsLocked;
            options.threadsBackground = defaults.threadsBackground;
            options.affinityBurst = defaults.affinityBurst;
            this.reload();
        })));
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

    private void addComponentsLaunching() {
        JPanel panel = this.createNewOptionsPanel("Launching");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Program Launching")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Right click for action menu")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        DefaultListModel<String> model = new DefaultListModel<>();
        options.launchingProgramPaths.forEach(model::addElement);

        JList<String> programList = new JList<>(model);
        programList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    // If right click is over already selected item(s) do nothing, but if right click on unselected
                    // item, select it and unselect previous selected.
                    int clickIndex = programList.locationToIndex(e.getPoint());
                    if (!programList.isSelectedIndex(clickIndex)) {
                        programList.setSelectedIndex(clickIndex);
                    }

                    JPopupMenu menu = new JPopupMenu();

                    JMenuItem add = new JMenuItem("Add");
                    add.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            OptionsGUI.this.browseForLauncherProgram();
                            OptionsGUI.this.reload();
                        }
                    });

                    JMenuItem remove = new JMenuItem("Remove");
                    remove.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            List<String> paths = programList.getSelectedValuesList();
                            paths.forEach(path -> options.launchingProgramPaths.remove(path));
                            OptionsGUI.this.reload();
                        }
                    });

                    menu.add(add);
                    menu.add(remove);

                    menu.show(programList, e.getX(), e.getY());
                }
            }
        });

        JScrollPane sp = new JScrollPane(programList);

        panel.add(GUIUtil.leftJustify(sp));
    }

    private void browseForLauncherProgram() {
        FileDialog dialog = new FileDialog((Frame) null, "Julti: Choose Program");
        dialog.setMode(FileDialog.LOAD);
        dialog.setMultipleMode(true);
        dialog.setVisible(true);
        if (dialog.getFiles() != null) {
            for (File file : dialog.getFiles()) {
                JultiOptions.getJultiOptions().launchingProgramPaths.add(file.getAbsolutePath());
            }
        }
        dialog.dispose();
    }

    private void addComponentsOther() {
        JPanel panel = this.createNewOptionsPanel("Other");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Other Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("resetCounter", "Reset Counter", this, "", ResetCounter::updateFiles)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("MultiMC Executable Path (.exe):")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("multiMCPath", "MultiMC Executable Path", this)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Auto-detect..."), actionEvent -> {
            this.runMMCExecutableHelper();
            this.reload();
        })));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Launch Instances Offline", "launchOffline", b -> this.reload())));
        panel.add(GUIUtil.createSpacer());

        if (options.launchOffline) {
            panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("launchOfflineName", "Offline Name", this)));
            panel.add(GUIUtil.createSpacer());
        }

        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("launchDelay", "Delay Between Instance Launches", this, "ms")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Minimize Julti To System Tray", "Minimizing Julti will move it to an icon in the system tray (bottom right).", "minimizeToTray", JultiGUI.getJultiGUI().getJultiIcon()::setTrayIconListener)));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Enable Pre-Release Updates", "Update checking will also check for pre-releases. Checking this box will trigger an update check.", "usePreReleases", b -> {
            if (b) {
                new Thread(() -> UpdateUtil.tryCheckForUpdates(JultiGUI.getJultiGUI()), "update-checker").start();
            }
        })));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Enable Experimental Options", "enableExperimentalOptions", b -> {
            if (!b) {
                Julti.waitForExecute(() -> {
                    options.autoFullscreen = false;
                    options.showDebug = false;
                    options.pieChartOnLoad = false;
                    options.preventWindowNaming = false;
                    options.alwaysOnTopProjector = false;
                    options.minimizeProjectorWhenPlaying = false;
                    options.activateProjectorOnReset = false;
                    options.useAltSwitching = false;
                    options.allowResetDuringGenerating = false;
                    options.resizeableBorderless = false;
                    options.utilityMode = false;
                });
            }
            this.reload();
        })));
    }

    private void runMMCExecutableHelper() {
        List<String> appNames = Arrays.asList("multimc.exe,prismlauncher.exe".split(","));
        List<Path> possibleLocations = new ArrayList<>();
        Path userHome = Paths.get(System.getProperty("user.home"));
        possibleLocations.add(userHome.resolve("Desktop"));
        possibleLocations.add(userHome.resolve("Documents"));
        possibleLocations.add(userHome.resolve("AppData").resolve("Roaming"));
        possibleLocations.add(userHome.resolve("AppData").resolve("Local").resolve("Programs"));
        possibleLocations.add(userHome.resolve("Downloads"));
        for (File drive : File.listRoots()) {
            possibleLocations.add(Paths.get(drive.toString()));
        }

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
                this.browseForMMCExecutable();
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
            this.browseForMMCExecutable();
        } else {
            Path chosen = candidates.get(ans);
            JultiOptions.getJultiOptions().multiMCPath = chosen.toString();
        }
    }

    private void browseForMMCExecutable() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setDialogTitle("Julti: Choose MultiMC Executable");
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("Executables", "exe"));

        int val = jfc.showOpenDialog(this);
        if (val == JFileChooser.APPROVE_OPTION) {

            JultiOptions.getJultiOptions().multiMCPath = jfc.getSelectedFile().toPath().toString();
        }
    }

    private void addComponentsOBS() {
        JPanel panel = this.createNewOptionsPanel("OBS");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("OBS Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Invisible Dirt Covers", "invisibleDirtCovers")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Align Active Instance to Center", "Mainly for EyeZoom/TallMacro/stretched window users", "centerAlignActiveInstance", b -> this.reload())));
        if (options.centerAlignActiveInstance) {
            panel.add(GUIUtil.createSpacer());
            JPanel scalePanel = GUIUtil.createActiveInstanceScalePanel();
            panel.add(GUIUtil.leftJustify(scalePanel));
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Show Instance Number Indicators", "showInstanceIndicators")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("instanceSpacing", "Instance Spacing (Border)", this)));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Custom Wall Window", "Enable if a window other than the OBS Fullscreen Projector is being used", "useCustomWallWindow", b -> this.reload())));

        if (options.useCustomWallWindow) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("customWallNameFormat", "Custom Wall Name Format", this)));
        }

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("No settings here are required to use the OBS Link Script.")));
    }

    private void addComponentsHotkey() {
        JPanel panel = this.createNewOptionsPanel("Hotkeys");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Hotkeys")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("Right click to clear (disable) hotkey")));
        panel.add(GUIUtil.leftJustify(new JLabel("Checkboxes = Allow extra keys (ignore ctrl/shift/alt)")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("In-Game Hotkeys")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("resetHotkey", "Reset", true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("bgResetHotkey", "Background Reset", true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Wall Hotkeys")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallResetHotkey", "Full Reset", true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallSingleResetHotkey", "Reset Instance", true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallLockHotkey", "Lock Instance", true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallPlayHotkey", "Play Instance", true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallFocusResetHotkey", "Focus Reset", true)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("wallPlayLockHotkey", "Play Next Lock", true)));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Script Hotkeys")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createHotkeyChangeButton("cancelScriptHotkey", "Cancel Running Script", true)));

        for (String scriptName : ScriptManager.getHotkeyableScriptNames()) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createScriptHotkeyChangeButton(scriptName, this::reload)));
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
        this.tabNames.add(name);
        return panel;
    }

    private void addComponentsProfile() {
        JPanel panel = this.createNewOptionsPanel("Profile");

        panel.add(GUIUtil.leftJustify(new JLabel("Profile")));
        panel.add(GUIUtil.createSpacer());

        JComboBox<String> profileSelectBox = new JComboBox<>(JultiOptions.getProfileNames());
        GUIUtil.setActualSize(profileSelectBox, 200, 22);
        profileSelectBox.addActionListener(e -> {
            changeProfile(profileSelectBox.getSelectedItem().toString());
            this.reload();
        });

        panel.add(GUIUtil.leftJustify(profileSelectBox));
        panel.add(GUIUtil.createSpacer());

        OptionsGUI thisGUI = this;
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Remove"), actionEvent -> {
            String toRemove = JultiOptions.getSelectedProfileName();
            if (0 != JOptionPane.showConfirmDialog(thisGUI, "Are you sure you want to remove the profile \"" + toRemove + "\"?", "Julti: Remove Profile", JOptionPane.YES_NO_OPTION)) {
                return;
            }
            String switchTo = "";
            for (String name : JultiOptions.getProfileNames()) {
                if (!name.equals(toRemove)) {
                    switchTo = name;
                    break;
                }
            }
            changeProfile(switchTo);
            JultiOptions.removeProfile(toRemove);
            this.reload();
        })));

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("New"), actionEvent -> {
            String newName = JOptionPane.showInputDialog(thisGUI, "Enter a new profile name:", "Julti: New Profile", JOptionPane.QUESTION_MESSAGE);
            if (newName != null) {
                if (Arrays.asList(JultiOptions.getProfileNames()).contains(newName)) {
                    JOptionPane.showMessageDialog(thisGUI, "Profile already exists!", "Julti: Cannot Create New Profile", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Julti.waitForExecute(() -> JultiOptions.getJultiOptions().tryCopyTo(newName));

                changeProfile(newName);
                this.reloadComponents();
            }
        })));
    }

    private void addComponentsWall() {
        JPanel panel = this.createNewOptionsPanel("Wall");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Wall Settings")));
        if (ResetHelper.getManager() instanceof MultiResetManager) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(new JLabel("Resetting mode is on Multi! A lot of these settings are only relevant to Wall mode.")));
        }
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Only Allow Focusing Loaded Instances", "If disabled, instances will be locked if you try to play them while they're still loading/previewing.", "wallLockInsteadOfPlay", b -> this.reload())));

        if (options.wallLockInsteadOfPlay) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Smart Switch", "If you try to play a loading instance, Julti will instead look for another available instance to play.", "wallSmartSwitch")));
        }


        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Bypass Wall", "Instead of returning to the wall on reset, you will be moved to the next available locked instance.", "wallBypass", b -> this.reload())));
        if (options.wallBypass) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Return to Wall if None Loaded", "If none of your locked instances are loaded, you will be returned to the wall on reset instead.", "returnToWallIfNoneLoaded")));
        }

        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Automatically Determine Wall Layout", "Disable to manually change rows & columns on the wall.", "autoCalcWallSize", b -> {
            this.reload();
            Julti.doLater(() -> ResetHelper.getManager().reload());
        })));
        if (!options.autoCalcWallSize) {
            panel.add(GUIUtil.createSpacer());
            panel.add(GUIUtil.leftJustify(new WallSizeComponent()));
        }

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Dirt Covers", "OBS will cover loading instances with a clean dirt image.", "doDirtCovers")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("wallResetCooldown", "Reset Cooldown", this)));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(new JLabel("The reset cooldown starts as soon as you can see the instance,")));
        panel.add(GUIUtil.leftJustify(new JLabel("this also accounts for appearance based on dirt covers and dynamic wall.")));


        if (!(ResetHelper.getManager() instanceof DynamicWallResetManager)) {
            return;
        }
        // Dynamic wall settings below

        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Dynamic Wall Settings")));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Replace Locked Instances", "If disabled, the wall will not be updated when an instance is locked.", "dwReplaceLocked")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("lockedInstanceSpace", "Locked Instance Space", this, "%")));
    }

    private void addComponentsReset() {
        JPanel panel = this.createNewOptionsPanel("Resetting");

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Reset Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(new JLabel("Style:")));

        JComboBox<String> resetStyleBox = new JComboBox<>(ResetHelper.getResetStyles().toArray(new String[0]));
        resetStyleBox.setSelectedItem(options.resetStyle);
        resetStyleBox.addActionListener(e -> {
            Julti.doLater(() -> {
                options.resetStyle = resetStyleBox.getSelectedItem().toString();
                ResetHelper.getManager().reload();
            });
            this.reload();
        });
        GUIUtil.setActualSize(resetStyleBox, 120, 23);

        panel.add(GUIUtil.leftJustify(resetStyleBox));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use F3", "useF3")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Unpause on Switch", "unpauseOnSwitch")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Coop Mode", "coopMode", b -> this.reload())));
        panel.add(GUIUtil.createSpacer());
        if (options.coopMode) {
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Cheats Enabled on Join", "coopModeCheats")));
            panel.add(GUIUtil.createSpacer());
        }
        panel.add(GUIUtil.leftJustify(GUIUtil.createValueChangerButton("clipboardOnReset", "Clipboard On Reset", this)));
    }

    public void reload() {
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

        JultiOptions options = JultiOptions.getJultiOptions();

        panel.add(GUIUtil.leftJustify(new JLabel("Window Settings")));
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Let Julti Manage Windows", "letJultiMoveWindows", b -> this.reload())));

        if (!options.letJultiMoveWindows) {
            return;
        }
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Borderless", "useBorderless", b -> this.reload())));
        panel.add(GUIUtil.createSpacer());

        if (!options.useBorderless || options.resizeableBorderless) {
            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Maximize When Playing", "maximizeWhenPlaying", b -> this.reload())));
            panel.add(GUIUtil.createSpacer());

            panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Maximize When Resetting", "maximizeWhenResetting", b -> this.reload())));
            panel.add(GUIUtil.createSpacer());
        }

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

            List<MonitorUtil.Monitor> sortedMonitors = new ArrayList<>();
            sortedMonitors.add(MonitorUtil.getPrimaryMonitor());
            Arrays.stream(monitors).filter(monitor -> !monitor.isPrimary).forEach(sortedMonitors::add);

            int i = 0;
            for (MonitorUtil.Monitor monitor : sortedMonitors) {
                buttons[i] = String.valueOf(i + 1);
                monitorOptionsText.append("\n#").append(++i);
                if (monitor.isPrimary) {
                    monitorOptionsText.append(" (Primary)");
                }
                monitorOptionsText.append(" - ").append("Size: ").append(monitor.width).append("x").append(monitor.height).append(", Position: (").append(monitor.x).append(",").append(monitor.y).append(")");
            }

            int ans = JOptionPane.showOptionDialog(thisGUI, "Choose a monitor:\n" + monitorOptionsText.toString().trim(), "Julti: Choose Monitor", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttons, null);
            if (ans == -1) {
                return;
            }
            MonitorUtil.Monitor monitor = sortedMonitors.get(ans);
            Julti.waitForExecute(() -> {
                options.windowPos = monitor.centerPosition;
                options.windowPosIsCenter = true;
                options.playingWindowSize = monitor.size;
            });
            windowOptions.reload();
            this.revalidate();
        })));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Optimize Resetting for Clarity"), actionEvent -> {
            // make resetting window size match wall source dimensions
            Dimension wallSize = OBSStateManager.getOBSStateManager().getOBSSceneSize();

            int totalRows = 2;
            int totalColumns = 2;
            if (options.autoCalcWallSize && options.resetStyle.equals("Wall")) {
                // auto calc for normal wall
                List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();
                totalRows = (int) Math.max(1, Math.ceil(Math.sqrt(instances.size())));
                totalColumns = (int) Math.max(1, Math.ceil(instances.size() / (float) totalRows));
            } else if (!options.autoCalcWallSize) {
                totalRows = Math.max(1, options.overrideRowsAmount);
                totalColumns = Math.max(1, options.overrideColumnsAmount);
            }

            if (options.resetStyle.equals("Dynamic Wall")) {
                wallSize.height -= (int) ((options.lockedInstanceSpace / 100) * wallSize.height);
            }

            // Using floats here so there won't be any gaps in the wall after converting back to int
            float iWidth = wallSize.width / (float) totalColumns;
            float iHeight = wallSize.height / (float) totalRows;

            if (!options.useBorderless) {
                iWidth += 16;
                iHeight += 39;
            }

            int[] iSize = {(int) iWidth - 2 * options.instanceSpacing, (int) iHeight - 2 * options.instanceSpacing};
            Julti.waitForExecute(() -> {
                options.resettingWindowSize = iSize;
            });
            windowOptions.reload();
            this.revalidate();
        })));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Optimize Resetting for FOV"), actionEvent -> {
            MonitorUtil.Monitor[] monitors = MonitorUtil.getAllMonitors();
            MonitorUtil.Monitor playingMonitor = null;
            for (MonitorUtil.Monitor monitor : monitors) {
                if (monitors[0].bounds.contains(new Point(options.windowPos[0], options.windowPos[1]))) {
                    playingMonitor = monitor;
                    break;
                }
            }
            if (playingMonitor == null) {
                return;
            }

            int[] iSize = {playingMonitor.size[0], (int) (playingMonitor.size[1] / 2.5)};
            Julti.waitForExecute(() -> {
                options.resettingWindowSize = iSize;
            });
            windowOptions.reload();
            this.revalidate();
        })));
        panel.add(GUIUtil.createSpacer());

        panel.add(GUIUtil.createSeparator());
        panel.add(GUIUtil.createSpacer());
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Prepare Window on Lock", "When locking, set the size of the instance to the \"Playing\" size, making switching quicker.", "prepareWindowOnLock")));
    }

    public void openTab(String tabName) {
        this.getTabbedPane().setSelectedIndex(this.tabNames.indexOf(tabName));
    }

    public void setScroll(int scroll) {
        ((JScrollPane) this.getTabbedPane().getSelectedComponent()).getVerticalScrollBar().setValue(scroll);
    }

    public boolean isClosed() {
        return this.closed;
    }

    private void onClose() {
        this.closed = true;
        Julti.doLater(() -> {
            if (!JultiOptions.getJultiOptions().utilityMode) {
                OBSStateManager.getOBSStateManager().tryOutputLSInfo();
                MistakesUtil.checkStartupMistakes();
            }
            SleepBGUtil.disableLock();
            Julti.resetInstancePositions();
        });
    }
}
