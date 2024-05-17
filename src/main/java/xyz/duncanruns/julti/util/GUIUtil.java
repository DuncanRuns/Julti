package xyz.duncanruns.julti.util;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
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
import java.util.concurrent.atomic.AtomicInteger;
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
        return createValueChangerButton(optionName, displayName, parent, valueSuffix, null);
    }

    public static JButton createValueChangerButton(final String optionName, final String displayName, final Component parent, final String valueSuffix, final Runnable afterSet) {
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
            boolean success = queueOptionChangeAndWait(optionName, ans);
            if (!success) {
                JOptionPane.showMessageDialog(parent, "Failed to set value! Perhaps you formatted it incorrectly.", "Julti: Set Option Failure", JOptionPane.ERROR_MESSAGE);
            }
            button.setText(buttonTextGetter.get());
            if (success && afterSet != null) {
                afterSet.run();
            }
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

    public static JComponent createFileSelectButton(final Component parent, final String optionName, final String fileType, Path startingLocation, boolean allowFoldersToo) {
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
            jfc.setFileSelectionMode(allowFoldersToo ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY);
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

        JCheckBox checkBox = createCheckBox("", "Allow Extra Keys (Ignore Ctrl/Shift/Alt)", data.ignoreModifiers, aBoolean -> {
            data.ignoreModifiers = !data.ignoreModifiers;
            Julti.waitForExecute(() -> JultiOptions.getJultiOptions().setScriptHotkey(data));
            reloadFunction.run();
            HotkeyManager.getHotkeyManager().reloadHotkeys();
        });
        return asHotkeyPanel(button, checkBox);
    }

    public static JCheckBox createCheckBox(String label, String desc, boolean defaultValue, Consumer<Boolean> onValueChange) {
        AtomicInteger toolTipYOff = new AtomicInteger();
        JCheckBox jCheckBox = new JCheckBox() {
            @Override
            public Point getToolTipLocation(MouseEvent event) {
                Point point = event.getPoint();
                return new Point(point.x + 10, point.y + toolTipYOff.get());
            }
        };
        jCheckBox.setSelected(defaultValue);
        jCheckBox.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onValueChange.accept(jCheckBox.isSelected());
            }
        });

        // Set tooltips if description is specified
        if (!desc.isEmpty()) {
            String text = label.isEmpty() ? "" : label + ": ";
            text += desc;
            FontMetrics fontMetrics = jCheckBox.getFontMetrics(jCheckBox.getFont());
            int textPixLength = fontMetrics.stringWidth(text);
            int maxWidth = 400;
            int textHeight = fontMetrics.getHeight() * (textPixLength / maxWidth);
            toolTipYOff.set(-textHeight - 25);
            int width = Math.min(textPixLength, maxWidth);
            jCheckBox.setToolTipText("<html><p width=\"" + width + "\">" + text + "</p></html>");
        }
        jCheckBox.setText(label);
        return jCheckBox;
    }

    public static JComponent createHotkeyChangeButton(final String optionName, String hotkeyName, boolean includeIMOption) {
        JButton button = new JButton();
        final String hotkeyPrefix = hotkeyName + (hotkeyName.isEmpty() ? "" : ": ");
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == 3) {
                    // Clear hotkey on right click
                    Hotkey hotkey = Hotkey.empty();
                    queueOptionChangeAndWait(optionName, hotkey.getKeys());
                    button.setText(hotkeyPrefix + Hotkey.formatKeys(hotkey.getKeys()));
                    HotkeyManager.getHotkeyManager().reloadHotkeys();
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

        JCheckBox checkBox = createCheckBoxFromOption("", "Allow Extra Keys (Ignore Ctrl/Shift/Alt)", optionName + "IM", b -> HotkeyManager.getHotkeyManager().reloadHotkeys());

        return asHotkeyPanel(button, checkBox);
    }

    private static JPanel asHotkeyPanel(JButton button, JCheckBox checkBox) {
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel.add(button, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel.add(checkBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel.add(new Spacer(), new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        return panel;
    }

    public static JCheckBox createCheckBoxFromOption(String label, String desc, String optionName, Consumer<Boolean> afterSet) {
        return createCheckBox(label, desc, (Boolean) JultiOptions.getJultiOptions().getValue(optionName), val -> {
            queueOptionChangeAndWait(optionName, val);
            if (afterSet != null) {
                afterSet.accept(val);
            }
        });
    }

    public static JCheckBox createCheckBoxFromOption(String label, String optionName, Consumer<Boolean> afterSet) {
        return createCheckBoxFromOption(label, "", optionName, afterSet);
    }

    public static JCheckBox createCheckBoxFromOption(String label, String desc, String optionName) {
        return createCheckBoxFromOption(label, desc, optionName, null);
    }

    public static JCheckBox createCheckBoxFromOption(String label, String optionName) {
        return createCheckBoxFromOption(label, "", optionName);
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
        slider.addChangeListener(e -> {
            int newCurrent = slider.getValue();
            queueOptionChangeAndWait(optionName, newCurrent / 100f);
            label.setText("Volume (" + newCurrent + "%)");
        });

        GUIUtil.setActualSize(slider, 200, 23);
        panel.add(slider);
        panel.add(label);

        return panel;
    }

    public static JPanel createActiveInstanceScalePanel() {
        JPanel scalePanel = new JPanel();
        scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.X_AXIS));
        scalePanel.add(new JLabel("Active Instance Scaling: "));
        scalePanel.add(createValueChangerButton("centerAlignScaleX", "X", scalePanel));
        scalePanel.add(createValueChangerButton("centerAlignScaleY", "Y", scalePanel));
        return scalePanel;
    }

    public static void forAllComponents(final Container container, Consumer<Component> consumer) {
        consumer.accept(container);
        for (Component comp : container.getComponents()) {
            consumer.accept(comp);
            if (comp instanceof Container) {
                forAllComponents((Container) comp, consumer);
            }
        }
    }
}
