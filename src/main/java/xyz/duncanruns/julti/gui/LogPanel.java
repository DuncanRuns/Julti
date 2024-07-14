package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import com.formdev.flatlaf.ui.FlatMarginBorder;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.management.LogReceiver;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.concurrent.atomic.AtomicInteger;

public class LogPanel extends JPanel {
    private final static int MAX_LOG_CHARS = 3000; // arbitrary number, can be changed

    public LogPanel() {
        this.setupWindow();
    }

    private void setupWindow() {
        this.setLayout(new GridBagLayout());
        this.createTextArea();
        this.createCommandLine();
        this.setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));
    }

    private void createTextArea() {
        JTextArea textArea = new JTextArea();
        AtomicInteger totalChars = new AtomicInteger();
        LogReceiver.setLogConsumer(s -> {
            if (totalChars.get() > 0) {
                s = "\n" + s;
            }
            textArea.append(s);
            totalChars.addAndGet(s.length());
            if (totalChars.get() > MAX_LOG_CHARS) {
                // We could just remove totalChars.get() - MAX_LOG_CHARS, but that could cut off some lines, so lets cut it off near at a nearby newline
                // This means once MAX_LOG_CHARS is reached, we actually stay a tiny bit over it
                int toRemove;
                try {
                    toRemove = textArea.getText(0, totalChars.get() - MAX_LOG_CHARS).lastIndexOf("\n");
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
                if (toRemove == -1) {
                    return;
                }
                toRemove++; // Include the newline itself for removal, as replaceRange's end is exclusive
                textArea.replaceRange(null, 0, toRemove);
                totalChars.addAndGet(-toRemove);
            }
        });
        textArea.setEditable(false);
        textArea.setBorder(new FlatBorder());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        ((DefaultCaret) textArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        this.add(new JScrollPane(textArea), new GridBagConstraints(0, 0, 1, 1, 1, 1, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
    }

    private void createCommandLine() {
        JTextField commandLine = new JTextField("Enter commands here...");
        commandLine.addActionListener(e -> {
            String command = e.getActionCommand();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            String finalCommand = command;
            new Thread(() -> {
                for (String s : finalCommand.split(";")) {
                    CommandManager.getMainManager().runCommand(s);
                }
            }, "julti-command-line").start();
            commandLine.setText("");
        });
        commandLine.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (commandLine.getText().trim().equals("Enter commands here...")) {
                    commandLine.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (commandLine.getText().trim().equals("")) {
                    commandLine.setText("Enter commands here...");
                }
            }
        });
        commandLine.setBorder(new FlatBorder());
        this.add(commandLine, new GridBagConstraints(0, 1, 1, 1, 1, 0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
    }
}
