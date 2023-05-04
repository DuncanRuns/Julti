package xyz.duncanruns.julti.gui;

import org.kohsuke.github.GHAsset;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

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

    public void download(GHAsset asset) throws IOException {
        URL url = new URL(asset.getBrowserDownloadUrl());
        URLConnection connection = url.openConnection();
        connection.connect();
        int fileSize = connection.getContentLength();
        this.bar.setMaximum(fileSize);
        this.bar.setValue(0);
        int i = 0;
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(asset.getName())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                i += bytesRead;
                if (i >= 102400) {
                    this.bar.setValue(this.bar.getValue() + i);
                    i = 0;
                }
            }
        }
    }
}
