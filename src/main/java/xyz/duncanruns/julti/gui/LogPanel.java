package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import com.formdev.flatlaf.ui.FlatMarginBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.util.LogReceiver;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.PrintStream;

public class LogPanel extends JPanel {
    private final Julti julti;
    private final PrintStream stdout;

    public LogPanel(Julti julti) {
        stdout = System.out;
        this.julti = julti;
        setupWindow();
    }

    private void setupWindow() {
        setLayout(new GridBagLayout());
        createTextArea();
        createCommandLine();
        setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));
    }

    private void createTextArea() {
        JTextArea textArea = new JTextArea();
        LogReceiver.setLogConsumer(s -> {
            if (!textArea.getText().isEmpty()) {
                textArea.append("\n");
            }
            textArea.append(s);
        });
        textArea.setEditable(false);
        textArea.setBorder(new FlatBorder());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        ((DefaultCaret) textArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        add(new JScrollPane(textArea), new GridBagConstraints(0, 0, 1, 1, 1, 1, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
    }

    private void createCommandLine() {
        JTextField commandLine = new JTextField("Enter commands here...");
        commandLine.addActionListener(e -> {
            Thread.currentThread().setName("julti-gui");
            String command = e.getActionCommand();
            if (command.startsWith("/")) command = command.substring(1);
            julti.runCommand(command);
            commandLine.setText("");
        });
        commandLine.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (commandLine.getText().trim().equals("Enter commands here..."))
                    commandLine.setText("");
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (commandLine.getText().trim().equals("")) {
                    commandLine.setText("Enter commands here...");
                }
            }
        });
        commandLine.setBorder(new FlatBorder());
        add(commandLine, new GridBagConstraints(0, 1, 1, 1, 1, 0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
    }
}
