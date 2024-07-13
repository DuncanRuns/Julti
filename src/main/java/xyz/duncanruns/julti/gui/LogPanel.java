package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import com.formdev.flatlaf.ui.FlatMarginBorder;
import com.google.common.collect.EvictingQueue;
import xyz.duncanruns.julti.command.CommandManager;
import xyz.duncanruns.julti.management.LogReceiver;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class LogPanel extends JPanel {

    private final static int MAX_LOG_ENTRIES = 2000; // arbitrary number, can be changed
    private final EvictingQueue<String> logs = EvictingQueue.create(MAX_LOG_ENTRIES);

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
        LogReceiver.setLogConsumer(s -> {
            logs.add(s);

            textArea.setText(null);
            if (logs.size() >= MAX_LOG_ENTRIES) {
                textArea.setText("Logs have been truncated. To view the full logs, go to Options > Other > View Julti Logs.\n\n");
            }
            textArea.append(String.join("\n", logs));
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
