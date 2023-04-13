package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.resetting.ResetHelper;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;

public class WallSizeComponent extends JPanel {

    public WallSizeComponent() {
        this.setLayout(new BoxLayout(this, 1));

        this.reload();
    }

    private static JPanel getSizePanel() {
        JPanel sizePanel = new JPanel();
        sizePanel.setLayout(new BoxLayout(sizePanel, 0));
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField rowField = new JFormattedTextField(formatter);
        JFormattedTextField columnField = new JFormattedTextField(formatter);
        GUIUtil.setActualSize(rowField, 50, 23);
        GUIUtil.setActualSize(columnField, 50, 23);
        sizePanel.add(rowField);
        sizePanel.add(columnField);
        JultiOptions options = JultiOptions.getInstance();
        rowField.setValue(options.overrideRowsAmount);
        columnField.setValue(options.overrideColumnsAmount);

        KeyListener listener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                this.update();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                this.update();
            }

            private void update() {
                int x = (int) rowField.getValue();
                int y = (int) columnField.getValue();
                Julti.waitForExecute(() -> {
                    JultiOptions options = JultiOptions.getInstance();
                    options.overrideRowsAmount = x;
                    options.overrideColumnsAmount = y;
                    Julti.doLater(() -> ResetHelper.getManager().reload());
                });
            }
        };
        rowField.addKeyListener(listener);
        columnField.addKeyListener(listener);
        GUIUtil.setActualSize(sizePanel, 200, 23);
        return sizePanel;
    }

    public void reload() {
        this.removeAll();
        this.add(GUIUtil.leftJustify(new JLabel("Wall Size (rows x columns)")));
        this.add(GUIUtil.leftJustify(getSizePanel()));
    }
}
