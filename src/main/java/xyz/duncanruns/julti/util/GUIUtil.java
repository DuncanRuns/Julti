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
        return createCheckBox(label, (Boolean) JultiOptions.getInstance().getValue(optionName), val -> {
            JultiOptions.getInstance().trySetValue(optionName, String.valueOf(val));
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

    public static JButton createHotkeyChangeButton(final String optionName, Julti julti) {
        JButton button = new JButton();
        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: find suitable shouldContinue
                HotkeyUtil.onNextHotkey(() -> true, hotkey -> {
                    JultiOptions.getInstance().trySetHotkey(optionName, hotkey.getKeys());
                    button.setText(HotkeyUtil.formatKeys(hotkey.getKeys()));
                    julti.setupHotkeys();
                });
                button.setText("...");
            }
        });
        button.setText(HotkeyUtil.formatKeys((List<Integer>) JultiOptions.getInstance().getValue(optionName)));
        return button;
    }
}
