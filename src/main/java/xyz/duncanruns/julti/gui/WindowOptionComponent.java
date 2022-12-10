package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;

public class WindowOptionComponent extends JPanel {

    public WindowOptionComponent() {
        setLayout(new BoxLayout(this, 1));

        reload();
    }

    public void reload() {
        removeAll();
        add(GUIUtil.leftJustify(new JLabel("Window Position")));
        add(GUIUtil.leftJustify(getPositionPanel()));
        add(GUIUtil.leftJustify(new JLabel("Window Size")));
        add(GUIUtil.leftJustify(getSizePanel()));

    }

    private static JPanel getPositionPanel() {
        JPanel positionPanel = new JPanel();
        positionPanel.setLayout(new BoxLayout(positionPanel, 0));
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField xField = new JFormattedTextField(formatter);
        JFormattedTextField yField = new JFormattedTextField(formatter);
        GUIUtil.setActualSize(xField, 50, 23);
        GUIUtil.setActualSize(yField, 50, 23);
        positionPanel.add(xField);
        positionPanel.add(yField);
        int[] windowPos = JultiOptions.getInstance().windowPos;
        xField.setValue(windowPos[0]);
        yField.setValue(windowPos[1]);
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
                JultiOptions.getInstance().windowPos[0] = (int) xField.getValue();
                JultiOptions.getInstance().windowPos[1] = (int) yField.getValue();
            }
        };
        xField.getDocument().addDocumentListener(documentListener);
        yField.getDocument().addDocumentListener(documentListener);
        GUIUtil.setActualSize(positionPanel, 200, 23);
        return positionPanel;
    }

    private static JPanel getSizePanel() {
        JPanel positionPanel = new JPanel();
        positionPanel.setLayout(new BoxLayout(positionPanel, 0));
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setCommitsOnValidEdit(true);
        JFormattedTextField xField = new JFormattedTextField(formatter);
        JFormattedTextField yField = new JFormattedTextField(formatter);
        GUIUtil.setActualSize(xField, 50, 23);
        GUIUtil.setActualSize(yField, 50, 23);
        positionPanel.add(xField);
        positionPanel.add(yField);
        final int[] windowSize = JultiOptions.getInstance().windowSize;
        xField.setValue(windowSize[0]);
        yField.setValue(windowSize[1]);
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
                windowSize[0] = (int) xField.getValue();
                windowSize[1] = (int) yField.getValue();
            }
        };
        xField.getDocument().addDocumentListener(documentListener);
        yField.getDocument().addDocumentListener(documentListener);
        GUIUtil.setActualSize(positionPanel, 200, 23);
        return positionPanel;
    }
}
