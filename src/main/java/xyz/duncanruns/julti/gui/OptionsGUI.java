package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.MonitorUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

public class OptionsGUI extends JFrame {
    private boolean closed = false;
    private final Julti julti;

    public OptionsGUI(Julti julti, JultiGUI gui) {
        this.julti = julti;
        setLocation(gui.getLocation());
        setupWindow();
        reloadComponents();
    }

    private static Component createSpacerBox() {
        return Box.createRigidArea(new Dimension(0, 5));
    }

    private void setupWindow() {
        getContentPane().removeAll();
        setLayout(new FlowLayout());
        setTitle("Julti Options");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        setSize(500, 500);
        setVisible(true);
    }

    private void reloadComponents() {
        getContentPane().removeAll();
        addComponentsProfile();
        addComponentsReset();
        addComponentsWindow();
        revalidate();
        repaint();
    }

    private void addComponentsProfile() {
        JPanel panel = new JPanel();
        JPanel outerPanel = new JPanel();
        outerPanel.setBorder(new FlatBorder());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(GUIUtil.leftJustify(new JLabel("Profile")));
        panel.add(createSpacerBox());

        JComboBox<String> profileSelectBox = new JComboBox<>(JultiOptions.getProfileNames());
        profileSelectBox.addActionListener(e -> {
            julti.changeProfile(profileSelectBox.getSelectedItem().toString());
            reloadComponents();
        });

        panel.add(GUIUtil.leftJustify(profileSelectBox));
        panel.add(createSpacerBox());

        OptionsGUI thisGUI = this;
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Remove"), actionEvent -> {
            String toRemove = JultiOptions.getSelectedProfileName();
            if (0 != JOptionPane.showConfirmDialog(thisGUI, "Are you sure you want to remove the profile \"" + toRemove + "\"?", "Julti: Remove Profile", JOptionPane.WARNING_MESSAGE))
                return;
            String switchTo = "";
            for (String name : JultiOptions.getProfileNames()) {
                if (!name.equals(toRemove)) {
                    switchTo = name;
                    break;
                }
            }
            julti.changeProfile(switchTo);
            JultiOptions.removeProfile(toRemove);
            reloadComponents();
        })));

        panel.add(createSpacerBox());
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("New"), actionEvent -> {
            String newName = JOptionPane.showInputDialog(thisGUI, "Enter a new profile name:", "Julti: New Profile", JOptionPane.QUESTION_MESSAGE);
            if (newName != null) {
                if (Arrays.asList(JultiOptions.getProfileNames()).contains(newName)) {
                    JOptionPane.showMessageDialog(thisGUI, "Profile already exists!", "Julti: Cannot Create New Profile", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JultiOptions.getInstance().copyTo(newName);
                julti.changeProfile(newName);
                reloadComponents();
            }
        })));

        outerPanel.add(panel);
        add(outerPanel);
    }

    private void addComponentsReset() {
        JPanel panel = new JPanel();
        JPanel outerPanel = new JPanel();
        outerPanel.setBorder(new FlatBorder());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(GUIUtil.leftJustify(new JLabel("Reset Settings")));
        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Pause On Load", "pauseOnLoad")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use F3", "useF3")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Wall", "useWall")));
        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Wall One At A Time", "wallOneAtATime")));

        panel.add(createSpacerBox());

        panel.add(GUIUtil.leftJustify(new JLabel("Clipboard on Reset:")));

        JTextField corField = new JTextField();
        corField.setText(JultiOptions.getInstance().clipboardOnReset);
        corField.getDocument().addDocumentListener(new DocumentListener() {
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
                JultiOptions.getInstance().clipboardOnReset = corField.getText();
            }
        });

        panel.add(corField);

        outerPanel.add(panel);
        add(outerPanel);
    }

    private void addComponentsWindow() {
        JPanel panel = new JPanel();
        JPanel outerPanel = new JPanel();
        outerPanel.setBorder(new FlatBorder());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(GUIUtil.leftJustify(new JLabel("Window Settings")));
        panel.add(createSpacerBox());


        panel.add(GUIUtil.leftJustify(GUIUtil.createCheckBoxFromOption("Use Borderless", "useBorderless")));
        panel.add(createSpacerBox());

        OptionsGUI thisGUI = this;
        panel.add(GUIUtil.leftJustify(GUIUtil.getButtonWithMethod(new JButton("Choose Monitor"), actionEvent -> {
            MonitorUtil.Monitor[] monitors = MonitorUtil.getAllMonitors();
            StringBuilder monitorOptionsText = new StringBuilder();
            Object[] buttons = new Object[monitors.length];
            int i = 0;
            for (MonitorUtil.Monitor monitor : monitors) {
                buttons[i] = String.valueOf(i + 1);
                monitorOptionsText.append("\n#").append(++i).append(" - ").append("Size: ").append(monitor.width).append("x").append(monitor.height).append(", Position: (").append(monitor.x).append(",").append(monitor.y).append(")");
            }

            int ans = JOptionPane.showOptionDialog(thisGUI, "Choose a monitor:\n" + monitorOptionsText.toString().trim(), "Julti: Choose Monitor", -1, JOptionPane.QUESTION_MESSAGE, null, buttons, null);
            if (ans == -1) return;
            JultiOptions options = JultiOptions.getInstance();
            MonitorUtil.Monitor monitor = monitors[ans];
            options.windowPos = monitor.position;
            options.windowSize = monitor.size;
            reloadComponents();
        })));
        panel.add(createSpacerBox());

        panel.add(new WindowOptionComponent());

        outerPanel.add(panel);
        add(outerPanel);
    }

    public boolean isClosed() {
        return closed;
    }

    private void onClose() {
        closed = true;
        julti.reloadInstancePositions();
    }
}
