package xyz.duncanruns.julti.util;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class GUIUtil {
    private GUIUtil() {
    }

    public static <T extends JButton> T getButtonWithMethod(T t, Consumer<ActionEvent> actionEventConsumer) {
        t.addActionListener(e -> {
            Thread.currentThread().setName("julti-gui");
            actionEventConsumer.accept(e);
        });
        return t;
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

    public static JCheckBox createCheckBoxFromOption(String label, String optionName) {
        return createCheckBoxFromOption(label, optionName, null);
    }

    public static Component createSpacer() {
        return createSpacer(5);
    }

    public static Component createSpacer(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    public static Component createThreadsSlider(String displayName, String optionName) {
        JultiOptions options = JultiOptions.getInstance();
        int current = (Integer) options.getValue(optionName);

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
