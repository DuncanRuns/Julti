package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;

public class ScriptPanel extends JPanel {
    private final Julti julti;
    private final String name;

    public ScriptPanel(Julti julti, String name, byte hotkeyContext, Runnable onDelete) {
        this.julti = julti;
        this.name = name;
        this.setBorder(new FlatBorder());
        this.setLayout(new FlowLayout());
        JPanel panel = new JPanel();

        String contextText = "";
        switch (hotkeyContext) {
            case 0:
                contextText += "(Script only)";
                break;
            case 1:
                contextText += "(In-game hotkeyable)";
                break;
            case 2:
                contextText += "(Wall hotkeyable)";
                break;
            case 3:
                contextText += "(Hotkeyable anywhere)";
                break;
        }

        JLabel nameLabel = new JLabel(name);
        GUIUtil.setActualSize(nameLabel, 140, 20);
        panel.add(nameLabel);

        JLabel contextLabel = new JLabel(contextText);
        GUIUtil.setActualSize(contextLabel, 130, 20);
        panel.add(contextLabel);

        panel.add(GUIUtil.getButtonWithMethod(new JButton("Run"), a -> ScriptManager.runScript(julti, name)));
        panel.add(GUIUtil.getButtonWithMethod(new JButton("Delete"), a -> {
            if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the script \"" + name + "\"?", "Julti: Delete Script", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != 0) {
                return;
            }
            ScriptManager.removeScript(name);
            onDelete.run();
        }));

        this.add(panel);

        GUIUtil.setActualSize(this, 450, 42);
    }
}
