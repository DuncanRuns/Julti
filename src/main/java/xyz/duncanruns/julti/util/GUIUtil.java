package xyz.duncanruns.julti.util;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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


    public static JCheckBox createCheckBoxFromOption(String label, String optionName) {
        return createCheckBoxFromOption(label, optionName, null);
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

    public static Component leftJustify(Component component) {
        Box b = Box.createHorizontalBox();
        b.add(component);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    public static JButton createHotkeyChangeButton(final String optionName, String hotkeyName, Julti julti) {
        JButton button = new JButton();
        final String hotkeyPrefix = hotkeyName + (hotkeyName.equals("") ? "" : ": ");
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
        return button;
    }
}
