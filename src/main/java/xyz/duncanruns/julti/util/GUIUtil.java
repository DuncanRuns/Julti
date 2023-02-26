package xyz.duncanruns.julti.util;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.script.ScriptHotkeyData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
            Object val = JultiOptions.getInstance().getValue(optionName);
            return displayName + ": " + val + valueSuffix;
        };

        JButton button = new JButton(buttonTextGetter.get());
        return getButtonWithMethod(button, actionEvent -> {
            String ans = (String) JOptionPane.showInputDialog(parent, "Input a new value for " + displayName + ":", "Julti: Set Option", JOptionPane.QUESTION_MESSAGE, null, null, JultiOptions.getInstance().getValue(optionName).toString());
            if (ans == null) return;
            // If there is a suffix, the answer ends in the suffix, and the answer isn't just equal to the suffix
            if ((!valueSuffix.isEmpty()) && ans.endsWith(valueSuffix) && (!ans.equals(valueSuffix))) {
                // Shorten the answer by the length of the answer
                ans = ans.substring(0, ans.length() - valueSuffix.length());
            }
            if (!JultiOptions.getInstance().trySetValue(optionName, ans))
                JOptionPane.showMessageDialog(parent, "Failed to set value! Perhaps you formatted it incorrectly.", "Julti: Set Option Failure", JOptionPane.ERROR_MESSAGE);
            button.setText(buttonTextGetter.get());
        });
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

    public static JComponent createScriptHotkeyChangeButton(final String scriptName, Julti julti, Runnable reloadFunction) {

        ScriptHotkeyData data = JultiOptions.getInstance().getScriptHotkeyData(scriptName);

        JButton button = new JButton(scriptName + ": " + HotkeyUtil.formatKeys(data.keys));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) {
                    data.keys = Collections.emptyList();
                    JultiOptions.getInstance().setScriptHotkey(data);
                    button.setText(scriptName + ": " + HotkeyUtil.formatKeys(data.keys));
                    julti.setupHotkeys();
                }
            }
        });
        button.addActionListener(e -> {
            HotkeyUtil.onNextHotkey(() -> !julti.isStopped(), hotkey -> {
                data.keys = hotkey.getKeys();
                JultiOptions.getInstance().setScriptHotkey(data);
                button.setText(scriptName + ": " + HotkeyUtil.formatKeys(data.keys));
                julti.setupHotkeys();
            });
            button.setText(scriptName + ": ...");
        });


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JCheckBox checkBox = createCheckBox("", data.ignoreModifiers, aBoolean -> {
            data.ignoreModifiers = !data.ignoreModifiers;
            JultiOptions.getInstance().setScriptHotkey(data);
            reloadFunction.run();
            julti.setupHotkeys();
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

    public static JComponent createHotkeyChangeButton(final String optionName, String hotkeyName, Julti julti, boolean includeIMOption) {
        JButton button = new JButton();
        final String hotkeyPrefix = hotkeyName + (hotkeyName.equals("") ? "" : ": ");
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) {
                    HotkeyUtil.Hotkey hotkey = new HotkeyUtil.Hotkey(Collections.emptyList());
                    JultiOptions.getInstance().trySetHotkey(optionName, hotkey.getKeys());
                    button.setText(hotkeyPrefix + HotkeyUtil.formatKeys(hotkey.getKeys()));
                    julti.setupHotkeys();
                }
            }
        });
        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HotkeyUtil.onNextHotkey(() -> !julti.isStopped(), hotkey -> {
                    JultiOptions.getInstance().trySetHotkey(optionName, hotkey.getKeys());
                    button.setText(hotkeyPrefix + HotkeyUtil.formatKeys(hotkey.getKeys()));
                    julti.setupHotkeys();
                });
                button.setText(hotkeyPrefix + "...");
            }
        });
        button.setText(hotkeyPrefix + HotkeyUtil.formatKeys((List<Integer>) JultiOptions.getInstance().getValue(optionName)));
        button.setFocusable(false);

        if (!includeIMOption) return button;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JCheckBox checkBox = createCheckBoxFromOption("", optionName + "IM", b -> julti.setupHotkeys());
        checkBox.setToolTipText("Ignore Extra Keys");
        panel.add(checkBox);
        panel.add(button);

        return panel;
    }

    public static JCheckBox createCheckBoxFromOption(String label, String optionName, Consumer<Boolean> afterSet) {
        return createCheckBox(label, (Boolean) JultiOptions.getInstance().getValue(optionName), val -> {
            JultiOptions.getInstance().trySetValue(optionName, String.valueOf(val));
            if (afterSet != null)
                afterSet.accept(val);
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
        JultiOptions options = JultiOptions.getInstance();
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
            JultiOptions.getInstance().trySetValue(optionName, String.valueOf(newCurrent));
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
}
