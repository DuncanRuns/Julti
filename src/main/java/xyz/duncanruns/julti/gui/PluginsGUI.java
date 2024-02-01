package xyz.duncanruns.julti.gui;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.julti.plugin.PluginManager.LoadedJultiPlugin;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PluginsGUI extends JFrame {
    private boolean closed = false;
    private JPanel panel;

    public PluginsGUI() {
        Point location = JultiGUI.getJultiGUI().getLocation();
        this.setLocation(location.x, location.y + 30);
        this.setupWindow();
        this.reload();
    }

    private void setupWindow() {
        this.setLayout(null);
        this.setTitle("Julti Plugins");
        this.setIconImage(JultiGUI.getLogo());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                PluginsGUI.this.onClose();
            }
        });
        this.setSize(320, 500);
        this.setVisible(true);
        this.panel = new JPanel();
        this.panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(this.panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        this.setContentPane(scrollPane);
    }

    private void reload() {
        JScrollBar verticalScrollBar = ((JScrollPane) this.getContentPane()).getVerticalScrollBar();
        int i = verticalScrollBar.getValue();
        this.panel.removeAll();


        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Open Plugins Folder"), a -> {
            try {
                Path pluginsPath = PluginManager.getPluginsPath();
                if (!Files.exists(pluginsPath)) {
                    // being careful with the fucky directory making
                    new File((System.getProperty("user.home") + "/.Julti/plugins/").replace("\\", "/").replace("//", "/")).mkdirs();
                }
                Desktop.getDesktop().browse(JultiOptions.getJultiDir().resolve("plugins").toUri());
            } catch (IOException e) {
                Julti.log(Level.ERROR, "Failed to open instance folder:\n" + ExceptionUtil.toDetailedString(e));
            }
        }));

        this.panel.add(GUIUtil.leftJustify(buttonsPanel));

        this.panel.add(GUIUtil.createSpacer(15));

        List<LoadedJultiPlugin> loadedPlugins = PluginManager.getPluginManager().getLoadedPlugins();
        for (LoadedJultiPlugin loadedPlugin : loadedPlugins) {
            this.panel.add(GUIUtil.leftJustify(new PluginPanel(loadedPlugin)));
        }

        if (loadedPlugins.isEmpty()) {
            this.panel.add(GUIUtil.leftJustify(new JLabel("No plugins :(")));
        }

        verticalScrollBar.setValue(i);
        this.revalidate();
        this.repaint();
    }

    private void onClose() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }


}
