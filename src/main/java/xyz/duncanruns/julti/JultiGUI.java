package xyz.duncanruns.julti;

import xyz.duncanruns.julti.util.ResourceUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

public class JultiGUI extends JFrame implements WindowListener {
    private final Julti julti;

    public JultiGUI(Julti julti) {
        this.julti = julti;
        setupWindow();
        addWindowListener(this);
    }

    private void setupWindow() {
        setTitle("Julti");
        setVisible(true);
        setResizable(false);
        getContentPane().add(new Button());
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
