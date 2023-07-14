package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.messages.OptionChangeQMessage;
import xyz.duncanruns.julti.util.GUIUtil;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;

public class WindowOptionComponent extends JPanel {

    public WindowOptionComponent() {
        this.setLayout(new BoxLayout(this, 1));

        this.reload();
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
        int[] windowPos = JultiOptions.getJultiOptions().windowPos;
        xField.setValue(windowPos[0]);
        yField.setValue(windowPos[1]);
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                this.update();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                this.update();
            }

            private void update() {
                int[] value = {(int) xField.getValue(), (int) yField.getValue()};
                Julti.getJulti().queueMessageAndWait(new OptionChangeQMessage("windowPos", value));
            }
        };
        xField.addKeyListener(keyListener);
        yField.addKeyListener(keyListener);
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
        int[] windowSize = JultiOptions.getJultiOptions().playingWindowSize;
        xField.setValue(windowSize[0]);
        yField.setValue(windowSize[1]);
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                this.update();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                this.update();
            }

            private void update() {
                int[] value = {(int) xField.getValue(), (int) yField.getValue()};
                Julti.getJulti().queueMessageAndWait(new OptionChangeQMessage("playingWindowSize", value));
            }
        };
        xField.addKeyListener(keyListener);
        yField.addKeyListener(keyListener);
        GUIUtil.setActualSize(positionPanel, 200, 23);
        return positionPanel;
    }

    private static JPanel getRSizePanel() {
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
        int[] windowSize = JultiOptions.getJultiOptions().resettingWindowSize;
        xField.setValue(windowSize[0]);
        yField.setValue(windowSize[1]);
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                this.update();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                this.update();
            }

            private void update() {
                int[] value = {(int) xField.getValue(), (int) yField.getValue()};
                Julti.getJulti().queueMessageAndWait(new OptionChangeQMessage("resettingWindowSize", value));
            }
        };
        xField.addKeyListener(keyListener);
        yField.addKeyListener(keyListener);
        GUIUtil.setActualSize(positionPanel, 200, 23);
        return positionPanel;
    }

    public void reload() {
        this.removeAll();
        this.add(GUIUtil.leftJustify(new JLabel("Window Position")));
        this.add(GUIUtil.leftJustify(getPositionPanel()));
        this.add(GUIUtil.leftJustify(new JLabel("Playing Window Size")));
        this.add(GUIUtil.leftJustify(getSizePanel()));
        this.add(GUIUtil.leftJustify(new JLabel("Resetting Window Size")));
        this.add(GUIUtil.leftJustify(getRSizePanel()));
    }
}
