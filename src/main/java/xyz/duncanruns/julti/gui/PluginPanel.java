package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import xyz.duncanruns.julti.plugin.PluginManager.LoadedJultiPlugin;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings({"ConstantValue"})
public class PluginPanel extends JPanel {
    public PluginPanel(LoadedJultiPlugin plugin) {
        this.setBorder(new FlatBorder());
        this.setLayout(new GridLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayoutManager(1, 3, new Insets(5, 10, 5, 10), -1, -1));

        JLabel nameLabel = new JLabel(plugin.pluginData.name);
        GUIUtil.setActualSize(nameLabel, 140, 20);
        panel.add(nameLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));

        panel.add(new Spacer(), new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));

        panel.add(GUIUtil.getButtonWithMethod(new JButton(plugin.pluginInitializer.getMenuButtonName()), actionEvent -> plugin.pluginInitializer.onMenuButtonPress()), new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        this.add(panel);

        GUIUtil.setActualSize(this, 280, 42);
    }
}
