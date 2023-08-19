package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.julti.plugin.PluginManager.LoadedJultiPlugin;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings({"ConstantValue"})
public class PluginPanel extends JPanel {
    public PluginPanel(LoadedJultiPlugin plugin) {
        this.setBorder(new FlatBorder());
        this.setLayout(new FlowLayout());
        JPanel panel = new JPanel();

        JLabel nameLabel = new JLabel(plugin.pluginData.name);
        GUIUtil.setActualSize(nameLabel, 140, 20);
        panel.add(nameLabel);

        panel.add(GUIUtil.getButtonWithMethod(new JButton(plugin.pluginInitializer.getMenuButtonName()), actionEvent -> plugin.pluginInitializer.onMenuButtonPress()));
        this.add(panel);

        GUIUtil.setActualSize(this, 280, 42);
    }
}
