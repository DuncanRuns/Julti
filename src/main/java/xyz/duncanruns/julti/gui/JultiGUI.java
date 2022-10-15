package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JultiGUI extends JFrame {
    private final Julti julti;
    private boolean closed;
    private ControlPanel controlPanel;

    public JultiGUI(Julti julti) {
        this.julti = julti;
        closed = false;
        setLayout(new GridBagLayout());
        setupComponents();
        setupWindow();
    }

    private void setupComponents() {
        add(new LogPanel(julti), new GridBagConstraints(
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

        controlPanel = new ControlPanel(julti, this);
        add(controlPanel, new GridBagConstraints(
                1,
                0,
                1,
                1,
                0,
                0,
                11,
                0,
                new Insets(0, 0, 0, 0),
                0,
                0
        ));

        add(new InstancesPanel(julti, () -> isActive() || isOptionsActive(), this::isClosed), new GridBagConstraints(
                0,
                1,
                2,
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
        setSize(672, 378);
        setTitle("Julti");
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
    }

    private boolean isOptionsActive() {
        OptionsGUI optionsGUI = controlPanel.getOptionsGUI();
        return optionsGUI != null && (!optionsGUI.isClosed()) && optionsGUI.isActive();
    }

    private boolean isClosed() {
        return closed;
    }

    private void onClose() {
        closed = true;
        julti.stop();
        System.exit(0);
    }
}
