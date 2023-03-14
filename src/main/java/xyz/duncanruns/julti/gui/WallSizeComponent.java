package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;

public class WallSizeComponent extends JPanel {
    public WallSizeComponent() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.reload();
    }

    public void reload() {
        this.removeAll();
        this.add(GUIUtil.leftJustify(new JLabel("Wall Size (rows x columns)")));
        this.add(GUIUtil.leftJustify(getSizePanel()));
    }

    private static JPanel getSizePanel() {
        JPanel sizePanel = new JPanel();
        sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.X_AXIS));
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
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                this.update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                this.update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                this.update();
            }

            private void update() {
                JultiOptions options = JultiOptions.getInstance();
                options.overrideRowsAmount = (int) rowField.getValue();
                options.overrideColumnsAmount = (int) columnField.getValue();
            }
        };
        rowField.getDocument().addDocumentListener(documentListener);
        columnField.getDocument().addDocumentListener(documentListener);
        GUIUtil.setActualSize(sizePanel, 200, 23);
        return sizePanel;
    }
}
