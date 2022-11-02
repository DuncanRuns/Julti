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
        setLayout(new BoxLayout(this, 1));

        reload();
    }

    public void reload() {
        removeAll();
        add(GUIUtil.leftJustify(new JLabel("Wall Size (rows x columns)")));
        add(GUIUtil.leftJustify(getSizePanel()));
    }

    private static JPanel getSizePanel() {
        JPanel positionPanel = new JPanel();
        positionPanel.setLayout(new BoxLayout(positionPanel, 0));
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField rowField = new JFormattedTextField(formatter);
        JFormattedTextField columnField = new JFormattedTextField(formatter);
        positionPanel.add(rowField);
        positionPanel.add(columnField);
        JultiOptions options = JultiOptions.getInstance();
        rowField.setValue(options.overrideRowsAmount);
        columnField.setValue(options.overrideColumnsAmount);
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                JultiOptions options = JultiOptions.getInstance();
                options.overrideRowsAmount = (int) rowField.getValue();
                options.overrideColumnsAmount = (int) columnField.getValue();
            }
        };
        rowField.getDocument().addDocumentListener(documentListener);
        columnField.getDocument().addDocumentListener(documentListener);
        return positionPanel;
    }
}
