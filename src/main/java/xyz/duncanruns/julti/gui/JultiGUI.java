package xyz.duncanruns.julti.gui;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.messages.OptionChangeQMessage;
import xyz.duncanruns.julti.messages.ShutdownQMessage;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.MonitorUtil.Monitor;
import xyz.duncanruns.julti.util.ResourceUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class JultiGUI extends JFrame {
    private static long lastUtilityKeyPress = 0;
    public static final KeyAdapter UTILITY_MODE_SWITCHER_LISTENER = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.isControlDown() && e.getKeyCode() == 85) {
                if (Math.abs(lastUtilityKeyPress - System.currentTimeMillis()) < 50) {
                    return;
                }
                lastUtilityKeyPress = System.currentTimeMillis();
                synchronized (Julti.getJulti()) {
                    JultiOptions options = JultiOptions.getJultiOptions();
                    options.utilityMode = !options.utilityMode;
                    if (options.utilityMode) {
                        Julti.log(Level.INFO, "Utility Mode enabled! Press Ctrl+U again to disable Utilty Mode.");
                    } else {
                        Julti.log(Level.INFO, "Utility Mode disabled.");
                    }
                    OptionsGUI.reloadIfOpen();
                    INSTANCE.getInstancesPanel().utilityCheckBox.setSelected(options.utilityMode);
                }
            }
        }
    };

    private static final JultiGUI INSTANCE = new JultiGUI();

    private boolean closed;
    private ControlPanel controlPanel;
    private InstancesPanel instancesPanel;
    private boolean updating = false;

    private JultiIcon trayIcon;

    public JultiGUI() {
        this.closed = false;
        this.setLayout(new GridBagLayout());
        this.setupComponents();
        this.setupWindow();
    }

    public static Image getLogo() {
        try {
            return ResourceUtil.getImageResource("/logo.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JultiGUI getJultiGUI() {
        return INSTANCE;
    }

    public static PluginsGUI getPluginsGUI() {
        return PluginsGUI.getGUI();
    }

    public ControlPanel getControlPanel() {
        return this.controlPanel;
    }

    public InstancesPanel getInstancesPanel() {
        return this.instancesPanel;
    }

    public JultiIcon getJultiIcon() {
        return this.trayIcon;
    }

    public void setVisible() {
        this.setVisible(true);
    }

    private void setupComponents() {
        this.add(new LogPanel(), new GridBagConstraints(
                0,
                0,
                1,
                1,
                1,
                1,
                10,
                1,
                new Insets(0, 0, 0, 0),
                0,
                0
        ));

        this.controlPanel = new ControlPanel();
        this.add(this.controlPanel, new GridBagConstraints(
                1,
                0,
                1,
                2,
                0,
                0,
                11,
                0,
                new Insets(0, 0, 0, 0),
                0,
                0
        ));

        this.instancesPanel = new InstancesPanel(() -> this.isActive() || this.isOptionsActive() || InstanceManager.getInstanceManager().getSelectedInstance() != null, this::isClosed);
        this.add(this.instancesPanel, new GridBagConstraints(
                0,
                1,
                1,
                1,
                0,
                0,
                10,
                1,
                new Insets(0, 0, 0, 0),
                0,
                0
        ));
    }

    private void setupWindow() {
        // ensure window is inbounds
        int[] lastGUIPos = JultiOptions.getJultiOptions().lastGUIPos;
        Monitor[] monitors = MonitorUtil.getAllMonitors();
        Boolean inbounds = false;
        for (Monitor monitor : monitors) {
            if (monitor.bounds.contains(lastGUIPos[0], lastGUIPos[1])) {
                inbounds = true;
                break;
            }
        }
        // if no monitors contain the last GUI position, reset the position to the primary monitor
        if (!inbounds) {
            lastGUIPos = MonitorUtil.getPrimaryMonitor().position;
        }
        this.setLocation(lastGUIPos[0], lastGUIPos[1]);

        this.setSize(800, 420);
        this.setTitle("Julti v" + Julti.VERSION);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JultiGUI.this.onClose();
            }
        });
        this.setIconImage(JultiGUI.getLogo());
        this.trayIcon = new JultiIcon(JultiGUI.getLogo());
        this.trayIcon.setListener(this, JultiOptions.getJultiOptions().minimizeToTray);

        this.addKeyListener(UTILITY_MODE_SWITCHER_LISTENER);
        GUIUtil.forAllComponents(this, component -> component.addKeyListener(UTILITY_MODE_SWITCHER_LISTENER));
    }

    private boolean isOptionsActive() {
        OptionsGUI optionsGUI = OptionsGUI.getGUI();
        return optionsGUI != null && (!optionsGUI.isClosed()) && optionsGUI.isActive();
    }

    private boolean isClosed() {
        return this.closed;
    }

    private void onClose() {
        this.closed = true;
        SystemTray.getSystemTray().remove(this.trayIcon);
        Julti.getJulti().queueMessage(new OptionChangeQMessage("lastGUIPos", new int[]{this.getLocation().x, this.getLocation().y}));
        Julti.getJulti().queueMessageAndWait(new ShutdownQMessage());
        if (!this.updating) {
            System.exit(0); // For some reason there are hanging threads left, not even started by Julti
        }
    }

    public void closeForUpdate() {
        this.updating = true;
        this.dispose();
    }
}
