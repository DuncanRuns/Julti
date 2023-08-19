package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.plugin.PluginManager;
import xyz.duncanruns.julti.plugin.PluginManager.LoadedJultiPlugin;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class PluginsGUI extends JFrame {
    private boolean closed = false;
    private JPanel panel;

    public PluginsGUI() {
        this.setLocation(JultiGUI.getJultiGUI().getLocation());
        this.setupWindow();
        this.reload();
    }

    private void setupWindow() {
        this.setLayout(null);
        this.setTitle("Julti Plugins");
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

        List<LoadedJultiPlugin> loadedPlugins = PluginManager.getPluginManager().getLoadedPlugins();
        for (LoadedJultiPlugin loadedPlugin : loadedPlugins) {
            this.panel.add(GUIUtil.leftJustify(new PluginPanel(loadedPlugin)));
        }

        if (loadedPlugins.isEmpty()) {
            this.panel.add(new JLabel("No plugins :("));
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
