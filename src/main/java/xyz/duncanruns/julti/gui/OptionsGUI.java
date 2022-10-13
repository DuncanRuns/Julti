package xyz.duncanruns.julti.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class OptionsGUI extends JFrame implements WindowListener {
    private boolean closed = false;

    public OptionsGUI() {
        setLayout(new FlowLayout());
        setTitle("Julti Options");
        add(new JLabel("The Options GUI has not been completed, please use the option and profile command."));
        addWindowListener(this);
        setSize(500,100);
        setVisible(true);
        setResizable(false);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        onClose();
    }

    private void onClose() {
        closed = true;
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
