package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.script.Script;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ScriptsGUI extends JFrame {
    private final Julti julti;
    private boolean closed = false;
    private JPanel panel;

    public ScriptsGUI(Julti julti, JultiGUI gui) {
        this.julti = julti;
        setLocation(gui.getLocation());
        setupWindow();
        reload();
    }

    private void setupWindow() {
        setLayout(null);
        setTitle("Julti Scripts");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        setSize(490, 500);
        setVisible(true);
        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        setContentPane(scrollPane);
    }

    private void reload() {
        JScrollBar verticalScrollBar = ((JScrollPane) getContentPane()).getVerticalScrollBar();
        int i = verticalScrollBar.getValue();
        panel.removeAll();

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Import Script"), a -> startImportScriptDialog()));
        buttonsPanel.add(GUIUtil.createSpacer(0));
        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Cancel Running Script"), a -> ScriptManager.requestCancel()));

        panel.add(GUIUtil.leftJustify(buttonsPanel));

        panel.add(GUIUtil.createSpacer(15));

        for (String name : ScriptManager.getScriptNames()) {
            panel.add(GUIUtil.leftJustify(new ScriptPanel(julti, name, ScriptManager.getHotkeyContext(name), this::reload)));
        }

        verticalScrollBar.setValue(i);
        revalidate();
        repaint();
    }

    private void onClose() {
        closed = true;
    }

    private void startImportScriptDialog() {
        String ans = JOptionPane.showInputDialog(this, "Enter the script string here:", "Julti: Import Script", JOptionPane.QUESTION_MESSAGE);
        if (ans == null) return;
        ans = ans.replace("\n", ";");

        if (ScriptManager.addScript(ans)) {
            reload();
            return;
        }


        if (Script.isSavableString(ans)) {
            if (ScriptManager.isDuplicateImport(ans)) {
                int replaceAns = JOptionPane.showConfirmDialog(this, "A script by the same name already exists, replace it?", "Julti: Import Script", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (replaceAns != 0) return;
                ScriptManager.forceAddScript(ans);
            } else {
                JOptionPane.showMessageDialog(this, "Could not import script. An unknown error occurred.", "Julti: Import Script Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Could not import script. The entered string was not a script string.", "Julti: Import Script Error", JOptionPane.ERROR_MESSAGE);
        }
        reload();
    }

    public boolean isClosed() {
        return closed;
    }


}
