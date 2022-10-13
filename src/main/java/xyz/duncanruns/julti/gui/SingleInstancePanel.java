package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;

import javax.swing.*;
import java.awt.*;

public class SingleInstancePanel extends JPanel {

    private final JLabel nameLabel = new JLabel("Unknown");
    private final JLabel statusLabel = new JLabel("Unknown");

    public SingleInstancePanel() {
        setBorder(new FlatBorder());
        setLayout(new FlowLayout());
        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(2, 1));

        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        panel.add(statusLabel);
        add(panel);
    }

    public void setInfo(String name, String status) {
        nameLabel.setText(name);
        statusLabel.setText(status);
    }
}