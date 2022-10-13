package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatMarginBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    private final Julti julti;
    private OptionsGUI optionsGUI = null;

    public ControlPanel(Julti julti) {
        this.julti = julti;
        setLayout(new GridLayout(0, 1, 5, 5));
        setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));
        add(GUIUtil.createButtonWithMethod(new JButton("Set Titles"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            julti.getInstanceManager().renameWindows();
        }));
        add(GUIUtil.createButtonWithMethod(new JButton("Close Instances"), actionEvent -> {
            Thread.currentThread().setName("julti-gui");
            for (MinecraftInstance instance : julti.getInstanceManager().getInstances()) {
                instance.closeWindow();
            }
        }));
        add(GUIUtil.createButtonWithMethod(new JButton("Options"), actionEvent -> {
            if (optionsGUI == null || optionsGUI.isClosed()) {
                optionsGUI = new OptionsGUI();
            } else {
                optionsGUI.requestFocus();
            }
        }));
    }
}
