package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.OfficialScriptsUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import java.util.function.Supplier;

public class OfficialScriptsBrowserGUI extends JFrame {
    private boolean closed = false;
    private JPanel panel;

    public OfficialScriptsBrowserGUI(Set<String> fileNames) {
        this.setupWindow();
        this.addComponents(fileNames);
    }

    private void addComponents(Set<String> fileNames) {
        this.panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Back to Scripts..."), a -> {
            this.dispose();
            ScriptsGUI.openGUI().setLocation(this.getLocation());
        })));
        this.panel.add(GUIUtil.createSpacer());
        fileNames.stream().sorted(String::compareTo).forEach(fileName -> {
            JPanel scriptPanel = new JPanel();
            scriptPanel.setBorder(new FlatBorder());
            scriptPanel.setLayout(new FlowLayout());
            String scriptName = fileName.substring(0, fileName.length() - 4);
            JLabel nameLabel = new JLabel(scriptName);
            GUIUtil.setActualSize(nameLabel, 270, 20);
            scriptPanel.add(nameLabel);
            Supplier<String> buttonTextSupplier = () -> ScriptManager.getScriptNames().contains(scriptName) ? "Update" : "Install";
            JButton button = new JButton(buttonTextSupplier.get());
            scriptPanel.add(GUIUtil.getButtonWithMethod(button, a -> {
                try {
                    OfficialScriptsUtil.downloadScript(fileName);
                    ScriptsGUI scriptsGUI = ScriptsGUI.getGUI();
                    if (scriptsGUI != null && !scriptsGUI.isClosed()) {
                        scriptsGUI.reload();
                    }
                    ScriptManager.runCustomization(scriptName);
                    button.setText(buttonTextSupplier.get());
                    this.requestFocus();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Failed to download script! (GitHub could be rate limiting you!)", "Julti: Script Download Failed", JOptionPane.ERROR_MESSAGE);
                    Julti.log(Level.ERROR, "Error while trying to download official script: " + ExceptionUtil.toDetailedString(e));
                }
            }));
            GUIUtil.setActualSize(scriptPanel, 370, 34);
            this.panel.add(GUIUtil.leftJustify(scriptPanel));
        });
    }

    private void setupWindow() {
        this.setLayout(null);
        this.setTitle("Julti Official Scripts Browser");
        this.setIconImage(JultiGUI.getLogo());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                OfficialScriptsBrowserGUI.this.onClose();
            }
        });
        this.setSize(420, 500);
        this.setVisible(true);
        this.panel = new JPanel();
        this.panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(this.panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        this.setContentPane(scrollPane);
    }

    @Override
    public void dispose() {
        this.onClose();
        super.dispose();
    }

    private void onClose() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }
}
