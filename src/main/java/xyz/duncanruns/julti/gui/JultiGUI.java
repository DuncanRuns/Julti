package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class JultiGUI extends JFrame implements WindowListener {
    private final Julti julti;
    private boolean closed;

    public JultiGUI(Julti julti) {
        this.julti = julti;
        closed = false;
        setupWindow();
        addWindowListener(this);
    }

    private void setupWindow() {

        setLayout(new GridBagLayout());

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

        add(new ControlPanel(julti), new GridBagConstraints(
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

        add(new InstancesPanel(julti, this::isActive, this::isClosed), new GridBagConstraints(
                0,
                1,
                2,
                1,
                0,
                0,
                10,
                0,
                new Insets(0, 0, 0, 0),
                0,
                0
        ));

        setSize(672, 378);
        setTitle("Julti");
        setVisible(true);
        //setResizable(false);
    }

    private boolean isClosed() {
        return closed;
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        julti.stop();
        System.exit(0);
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}
