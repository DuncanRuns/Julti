package xyz.duncanruns.julti.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public final class GUIUtil {
    private GUIUtil() {
    }

    public static <T extends JComponent> T createWithBorder(T t, int top, int left, int bottom, int right) {
        t.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return t;
    }

    public static <T extends JButton> T createButtonWithMethod(T t, Consumer<ActionEvent> actionEventConsumer) {
        t.addActionListener(actionEventConsumer::accept);
        return t;
    }
}
