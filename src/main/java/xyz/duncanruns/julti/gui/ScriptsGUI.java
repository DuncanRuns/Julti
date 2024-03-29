package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.script.ScriptManager;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("..."), a -> {
            // TODO: importing scripts
        }));
        buttonsPanel.add(GUIUtil.createSpacer(0));
        buttonsPanel.add(GUIUtil.getButtonWithMethod(new JButton("Cancel Running Scripts"), a -> ScriptManager.cancelAllScripts()));

        this.panel.add(GUIUtil.leftJustify(buttonsPanel));

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
