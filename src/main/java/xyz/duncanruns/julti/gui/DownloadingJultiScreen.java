package xyz.duncanruns.julti.gui;

import javax.swing.*;
import java.awt.*;

public class DownloadingJultiScreen extends JFrame {
    private final JProgressBar bar;

    public DownloadingJultiScreen(Point location) {
        this.setLayout(new GridBagLayout());
        JLabel text = new JLabel("Downloading Julti...");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        this.add(text, gbc);
        this.bar = new JProgressBar(0, 100);
        this.add(this.bar, gbc);


        this.setSize(300, 100);
        this.setTitle("Julti");
        this.setLocation(location);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);
    }

    public JProgressBar getBar() {
        return this.bar;
    }
}
