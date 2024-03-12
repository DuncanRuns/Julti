package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.script.Script;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;

public class ScriptPanel extends JPanel {

    public ScriptPanel(String name, byte hotkeyContext, Runnable onDelete) {
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

        panel.add(GUIUtil.getButtonWithMethod(new JButton("Run"), a -> Julti.doLater(() -> ScriptManager.runScript(name))));
        panel.add(GUIUtil.getButtonWithMethod(new JButton("Edit"), a -> this.suggestEdit(name, onDelete)));
        panel.add(GUIUtil.getButtonWithMethod(new JButton("Delete"), a -> this.suggestDelete(name, onDelete)));

        this.add(panel);

        GUIUtil.setActualSize(this, 530, 42);
    }

    private void suggestDelete(String name, Runnable onDelete) {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the script \"" + name + "\"?", "Julti: Delete Script", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != 0) {
            return;
        }
        ScriptManager.removeScript(name);
        onDelete.run();
    }

    private void suggestEdit(String name, Runnable onDelete) {
        String newSavableString = JOptionPane.showInputDialog(this, ScriptManager.getScript(name).getName(), ScriptManager.getScript(name).toSavableString());
        if (newSavableString == null || newSavableString.isEmpty()) {
            return;
        }
        if (!ScriptManager.forceAddScript(newSavableString)) {
            JOptionPane.showMessageDialog(this, "Could not edit script. The entered string was not a script string.", "Julti: Edit Script Error", JOptionPane.ERROR_MESSAGE);
        } else {
            if (!Script.fromSavableString(newSavableString).getName().equals(name)) { // If name changes in edit
                ScriptManager.removeScript(name); // Remove old script
                onDelete.run();
            }
        }
    }
}
