package xyz.duncanruns.julti.gui;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ScriptsGUI extends JFrame {
    private boolean closed = false;
    private JPanel panel;

    public ScriptsGUI() {
        Point location = JultiGUI.getJultiGUI().getLocation();
        this.setLocation(location.x, location.y + 30);
        this.setupWindow();
        this.reload();
    }

    private void setupWindow() {
        this.setLayout(null);
        this.setTitle("Julti Scripts");
        this.setIconImage(JultiGUI.getLogo());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ScriptsGUI.this.onClose();
            }
        });
        this.setSize(410, 500);
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

        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Import Legacy Script"), a -> {
            Runnable showInvalid = () -> JOptionPane.showMessageDialog(this, "Invalid legacy script code!", "Julti: Import Script Failed", JOptionPane.WARNING_MESSAGE);
            String out = JOptionPane.showInputDialog(this, "Input a legacy script import code:", "Julti: Import Legacy Script", JOptionPane.PLAIN_MESSAGE);
            if (out == null || out.isEmpty()) {
                return;
            }
            String[] parts = out.split(";");
            if (parts.length < 2) {
                showInvalid.run();
                return;
            }
            String scriptName = parts[0];
            try {
                Byte.parseByte(parts[1]);
            } catch (NumberFormatException e) {
                showInvalid.run();
                return;
            }

            if (!ScriptManager.getScriptNames().contains(scriptName) || 0 == JOptionPane.showConfirmDialog(this, "Overwrite existing script (" + scriptName + ")?", "Julti: Overwrite Legacy Script", JOptionPane.YES_NO_OPTION)) {
                try {
                    ScriptManager.deleteScript(scriptName);
                    ScriptManager.writeLegacyScript(out);
                    ScriptManager.reload();
                } catch (IOException e) {
                    Julti.log(Level.ERROR, "Failed to write script: " + ExceptionUtil.toDetailedString(e));
                }
                this.reload();
            }
        }));
        buttonsPanel.add(GUIUtil.createSpacer(0));
        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Cancel Running Scripts"), a -> ScriptManager.cancelAllScripts()));

        JPanel buttonsPanel2 = new JPanel();
        buttonsPanel2.setLayout(new BoxLayout(buttonsPanel2, BoxLayout.X_AXIS));

        buttonsPanel2.add(GUIUtil.getButtonWithMethod(new JButton("Open Scripts Folder"), a -> {
            try {
                Desktop.getDesktop().browse(ScriptManager.SCRIPTS_FOLDER.toUri());
            } catch (IOException ignored) {
            }
        }));
        buttonsPanel2.add(GUIUtil.createSpacer(0));
        buttonsPanel2.add(GUIUtil.getButtonWithMethod(new JButton("Reload"), a -> {
            ScriptManager.reload();
            this.reload();
        }));

        this.panel.add(GUIUtil.leftJustify(buttonsPanel));
        this.panel.add(GUIUtil.createSpacer());
        this.panel.add(GUIUtil.leftJustify(buttonsPanel2));

        this.panel.add(GUIUtil.createSpacer(15));

        this.panel.add(GUIUtil.leftJustify(new JLabel("(Right click for action menu)")));
        for (String name : ScriptManager.getScriptNames()) {
            this.panel.add(GUIUtil.leftJustify(new ScriptPanel(name, ScriptManager.getHotkeyContext(name), this::reload)));
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
