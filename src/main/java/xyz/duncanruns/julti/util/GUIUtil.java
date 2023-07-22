package xyz.duncanruns.julti.util;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.affinity.AffinityManager;
import xyz.duncanruns.julti.hotkey.Hotkey;
import xyz.duncanruns.julti.hotkey.HotkeyManager;
import xyz.duncanruns.julti.messages.OptionChangeQMessage;
import xyz.duncanruns.julti.script.ScriptHotkeyData;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GUIUtil {
    private GUIUtil() {
    }

    public static Component leftJustify(Component component) {
        Box b = Box.createHorizontalBox();
        b.add(component);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    public static JPanel withConstantSize(Component component, Dimension dimension) {
        JPanel panel = new JPanel();
        panel.getInsets().set(0, 0, 0, 0);
        panel.setSize(dimension);
        panel.setPreferredSize(dimension);
        panel.add(component);
        return panel;
    }

    public static JSeparator createSeparator() {
        JSeparator jSeparator = new JSeparator();
        jSeparator.setMaximumSize(new Dimension(10000, 2));
        return jSeparator;
    }

    public static JButton createValueChangerButton(final String optionName, String displayName, Component parent) {
        return createValueChangerButton(optionName, displayName, parent, "");
    }

    public static JButton createValueChangerButton(final String optionName, final String displayName, final Component parent, final String valueSuffix) {
        final Supplier<String> buttonTextGetter = () -> {
            Object val = JultiOptions.getJultiOptions().getValue(optionName);
            return (displayName.isEmpty() ? "" : (displayName + ": ")) + val + valueSuffix;
        };

        JButton button = new JButton(buttonTextGetter.get());
        return getButtonWithMethod(button, actionEvent -> {
            String ans = (String) JOptionPane.showInputDialog(parent, "Input a new value for " + displayName + ":", "Julti: Set Option", JOptionPane.QUESTION_MESSAGE, null, null, JultiOptions.getJultiOptions().getValue(optionName).toString());
            if (ans == null) {
                return;
            }
            // If there is a suffix, the answer ends in the suffix, and the answer isn't just equal to the suffix
            if ((!valueSuffix.isEmpty()) && ans.endsWith(valueSuffix) && (!ans.equals(valueSuffix))) {
                // Shorten the answer by the length of the answer
                ans = ans.substring(0, ans.length() - valueSuffix.length());
            }
            if (!queueOptionChangeAndWait(optionName, ans)) {
                JOptionPane.showMessageDialog(parent, "Failed to set value! Perhaps you formatted it incorrectly.", "Julti: Set Option Failure", JOptionPane.ERROR_MESSAGE);
            }
            button.setText(buttonTextGetter.get());
        });
    }

    private static boolean queueOptionChangeAndWait(String optionName, Object val) {
        return Julti.getJulti().queueMessageAndWait(new OptionChangeQMessage(optionName, val));
    }

    public static <T extends JButton> T getButtonWithMethod(T t, Consumer<ActionEvent> actionEventConsumer) {
        t.addActionListener(e -> {
            Thread.currentThread().setName("julti-gui");
            actionEventConsumer.accept(e);
        });
        return t;
    }

    public static void addMenuItem(JPopupMenu menu, String name, Action action) {
        JMenuItem item = new JMenuItem();
        item.setAction(action);
        item.setText(name);
        menu.add(item);
    }

    public static JComponent createFileSelectButton(final Component parent, final String optionName, final String fileType, Path startingLocation) {
        final JultiOptions options = JultiOptions.getJultiOptions();

        String currentValue = options.getValueString(optionName);
        JButton button = new JButton(currentValue.isEmpty() ? "No File Selected" : currentValue);

        Path fStartingLocation = startingLocation == null ? Paths.get(currentValue) : startingLocation;

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if ((!options.getValueString(optionName).isEmpty()) && e.getButton() == 3) {
                    int ans = JOptionPane.showConfirmDialog(parent, "Clear file selection?", "Julti: Clear file selection", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (ans == 0) {
                        queueOptionChangeAndWait(optionName, "");
                        button.setText("No File Selected");
                    }
                }
            }
        });
        button.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setDialogTitle("Julti: Choose Files");
            jfc.setAcceptAllFileFilterUsed(false);
            jfc.addChoosableFileFilter(new FileNameExtensionFilter(fileType, fileType));
            jfc.setCurrentDirectory(fStartingLocation.toFile());

            int val = jfc.showOpenDialog(parent);
            if (val == JFileChooser.APPROVE_OPTION) {
                String chosen = jfc.getSelectedFile().toPath().toString();
                queueOptionChangeAndWait(optionName, chosen);
                button.setText(chosen);
            }
        });
        return button;
    }

    public static JComponent createScriptHotkeyChangeButton(final String scriptName, Runnable reloadFunction) {

        ScriptHotkeyData data = JultiOptions.getJultiOptions().getScriptHotkeyData(scriptName);

        JButton button = new JButton(scriptName + ": " + Hotkey.formatKeys(data.keys));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == 3) {
                    data.keys = Collections.emptyList();
                    Julti.waitForExecute(() -> JultiOptions.getJultiOptions().setScriptHotkey(data));
                    button.setText(scriptName + ": " + Hotkey.formatKeys(data.keys));
                    HotkeyManager.getHotkeyManager().reloadHotkeys();
                }
            }
        });
        button.addActionListener(e -> {
            Hotkey.onNextHotkey(() -> Julti.getJulti().isRunning(), hotkey -> {
                data.keys = hotkey.getKeys();
                Julti.waitForExecute(() -> JultiOptions.getJultiOptions().setScriptHotkey(data));
                button.setText(scriptName + ": " + Hotkey.formatKeys(data.keys));
                HotkeyManager.getHotkeyManager().reloadHotkeys();
            });
            button.setText(scriptName + ": ...");
        });


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JCheckBox checkBox = createCheckBox("", data.ignoreModifiers, aBoolean -> {
            data.ignoreModifiers = !data.ignoreModifiers;
            Julti.waitForExecute(() -> JultiOptions.getJultiOptions().setScriptHotkey(data));
            reloadFunction.run();
            HotkeyManager.getHotkeyManager().reloadHotkeys();
        });
        checkBox.setToolTipText("Ignore Extra Keys");
        panel.add(checkBox);
        panel.add(button);

        return panel;
    }

    public static JCheckBox createCheckBox(String label, boolean defaultValue, Consumer<Boolean> onValueChange) {
        JCheckBox jCheckBox = new JCheckBox();
        jCheckBox.setSelected(defaultValue);
        jCheckBox.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onValueChange.accept(jCheckBox.isSelected());
            }
        });
        jCheckBox.setText(label);
        return jCheckBox;
    }

    public static JComponent createHotkeyChangeButton(final String optionName, String hotkeyName, boolean includeIMOption) {
        JButton button = new JButton();
        final String hotkeyPrefix = hotkeyName + (hotkeyName.equals("") ? "" : ": ");
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == 3) {
                    Hotkey hotkey = Hotkey.empty();
                    queueOptionChangeAndWait(optionName, hotkey.getKeys());

                    button.setText(hotkeyPrefix + Hotkey.formatKeys(hotkey.getKeys()));
                }
            }
        });
        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Julti julti = Julti.getJulti();
                Hotkey.onNextHotkey(julti::isRunning, hotkey -> {
                    queueOptionChangeAndWait(optionName, hotkey.getKeys());
                    button.setText(hotkeyPrefix + Hotkey.formatKeys(hotkey.getKeys()));
                    HotkeyManager.getHotkeyManager().reloadHotkeys();
                });
                button.setText(hotkeyPrefix + "...");
            }
        });
        button.setText(hotkeyPrefix + Hotkey.formatKeys((List<Integer>) JultiOptions.getJultiOptions().getValue(optionName)));
        button.setFocusable(false);

        if (!includeIMOption) {
            return button;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JCheckBox checkBox = createCheckBoxFromOption("", optionName + "IM", b -> HotkeyManager.getHotkeyManager().reloadHotkeys());
        checkBox.setToolTipText("Ignore Extra Keys");
        panel.add(checkBox);
        panel.add(button);

        return panel;
    }

    public static JCheckBox createCheckBoxFromOption(String label, String optionName, Consumer<Boolean> afterSet) {
        return createCheckBox(label, (Boolean) JultiOptions.getJultiOptions().getValue(optionName), val -> {
            queueOptionChangeAndWait(optionName, val);
            if (afterSet != null) {
                afterSet.accept(val);
            }
        });
    }

    public static JCheckBox createCheckBoxFromOption(String label, String optionName) {
        return createCheckBoxFromOption(label, optionName, null);
    }

    public static Component createSpacer() {
        return createSpacer(5);
    }

    public static Component createSpacer(int height) {
        return Box.createRigidArea(new Dimension(10, height));
    }

    public static Component createThreadsSlider(String displayName, String optionName) {
        JultiOptions options = JultiOptions.getJultiOptions();
        int current = (Integer) options.getValue(optionName);
        current = Math.max(1, Math.min(AffinityManager.AVAILABLE_THREADS, current));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel label = new JLabel();
        label.setText(displayName + " (" + current + ")");

        JSlider slider = new JSlider(JSlider.HORIZONTAL, 1, AffinityManager.AVAILABLE_THREADS, current);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);
        slider.addChangeListener(e -> {
            int newCurrent = slider.getValue();
            queueOptionChangeAndWait(optionName, newCurrent);
            label.setText(displayName + " (" + newCurrent + ")");
        });

        GUIUtil.setActualSize(slider, 200, 23);
        panel.add(slider);
        panel.add(label);

        return panel;
    }

    public static void setActualSize(Component component, int x, int y) {
        Dimension d = new Dimension(x, y);
        component.setSize(d);
        component.setMaximumSize(d);
        component.setPreferredSize(d);
    }

    public static JComponent createVolumeSlider(String optionName) {
        JultiOptions options = JultiOptions.getJultiOptions();
        int current = (int) (((Float) options.getValue(optionName)) * 100);
        current = Math.max(0, Math.min(100, current));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel label = new JLabel();
        label.setText("Volume (" + current + "%)");

        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, current);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);
        slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int newCurrent = slider.getValue();
                queueOptionChangeAndWait(optionName, newCurrent / 100f);
                label.setText("Volume (" + newCurrent + "%)");
            }
        });

        GUIUtil.setActualSize(slider, 200, 23);
        panel.add(slider);
        panel.add(label);

        return panel;
    }
}
