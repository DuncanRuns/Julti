package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.messages.OptionChangeQMessage;
import xyz.duncanruns.julti.messages.ShutdownQMessage;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.MonitorUtil.Monitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JultiGUI extends JFrame {
    private static final JultiGUI INSTANCE = new JultiGUI();

    private boolean closed;
    private ControlPanel controlPanel;
    private boolean updating = false;


    public JultiGUI() {
        this.closed = false;
        this.setLayout(new GridBagLayout());
        this.setupComponents();
        this.setupWindow();
    }

    public static JultiGUI getJultiGUI() {
        return INSTANCE;
    }

    public static PluginsGUI getPluginsGUI() {
        return getJultiGUI().getControlPanel().openPluginsGui();
    }

    public ControlPanel getControlPanel() {
        return this.controlPanel;
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

        this.add(new InstancesPanel(() -> this.isActive() || this.isOptionsActive() || InstanceManager.getInstanceManager().getSelectedInstance() != null, this::isClosed), new GridBagConstraints(
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
        this.setTitle("Julti");
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JultiGUI.this.onClose();
            }
        });
    }

    private boolean isOptionsActive() {
        OptionsGUI optionsGUI = this.controlPanel.getOptionsGUI();
        return optionsGUI != null && (!optionsGUI.isClosed()) && optionsGUI.isActive();
    }

    private boolean isClosed() {
        return this.closed;
    }

    private void onClose() {
        this.closed = true;
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
