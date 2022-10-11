package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class JultiGUI extends JFrame implements WindowListener {
    private final Julti julti;
    private final JPanel panel;

    public JultiGUI(Julti julti) {
        this.julti = julti;
        panel = new JPanel();
        setupWindow();
        addWindowListener(this);
    }

    private void setupWindow() {
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setLayout(new GridLayout(2, 2));

        add(panel, BorderLayout.CENTER);
        setTitle("Julti");
        pack();
        setVisible(true);
        setResizable(false);
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
