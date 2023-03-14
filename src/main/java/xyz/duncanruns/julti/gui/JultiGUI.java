package xyz.duncanruns.julti.gui;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.LogReceiver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JultiGUI extends JFrame {
    private static final Logger LOGGER = LogManager.getLogger("InstanceManager");
    private final Julti julti;
    private boolean closed;
    private ControlPanel controlPanel;

    public JultiGUI(Julti julti) {
        this.julti = julti;
        this.closed = false;
        this.setLayout(new GridBagLayout());
        this.setupComponents();
        this.setupWindow();
    }

    private void setupComponents() {
        this.add(new LogPanel(this.julti), new GridBagConstraints(
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

        this.controlPanel = new ControlPanel(this.julti, this);
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

        this.add(new InstancesPanel(this.julti, () -> isActive() || isOptionsActive(), this::isClosed), new GridBagConstraints(
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
        setSize(800, 420);
        int[] lastGUIPos = JultiOptions.getInstance().lastGUIPos;
        setLocation(lastGUIPos[0], lastGUIPos[1]);
        setTitle("Julti");
        setVisible(true);
        addWindowListener(new WindowAdapter() {
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
        JultiOptions.getInstance().lastGUIPos = new int[]{getLocation().x, getLocation().y};
        this.julti.stop();
        System.exit(0);
    }

    public static void log(Level level, String message) {
        LOGGER.log(level, message);
        LogReceiver.receive(level, message);
    }
}
