package xyz.duncanruns.julti.gui;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.messages.ShutdownQMessage;
import xyz.duncanruns.julti.util.MouseUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static java.awt.Frame.*;

public class JultiIcon extends TrayIcon {

    private PopupMenu menu;

    public JultiIcon(Image image) {
        super(image);

        // https://stackoverflow.com/questions/7461477/how-to-hide-a-jframe-in-system-tray-of-taskbar?noredirect=1&lq=1
        this.setImageAutoSize(true);

        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JultiGUI.getJultiGUI().setVisible(true);
                JultiGUI.getJultiGUI().setExtendedState(NORMAL);
            }
        });
        try {
            SystemTray.getSystemTray().add(this);
        } catch (AWTException ignored) {}

    }

    public void setListener(JultiGUI gui, boolean add) {
        for (WindowStateListener listener : gui.getWindowStateListeners()) {
            gui.removeWindowStateListener(listener);
        }
        if (!add) return;

        gui.addWindowStateListener(e -> {
            if (e.getNewState() == ICONIFIED) {
                gui.setVisible(false);
            }
            if (e.getNewState() == MAXIMIZED_BOTH || e.getNewState() == NORMAL) {
                gui.setVisible(true);
            }
        });
    }

    public void setTrayIconListener(boolean add) {
        this.setListener(JultiGUI.getJultiGUI(), add);
    }
}
