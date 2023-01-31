package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.GUIUtil;
import xyz.duncanruns.julti.util.SafeInstanceLauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class SingleInstancePanel extends JPanel implements MouseListener {

    private final JLabel nameLabel = new JLabel("Unknown");
    private final JLabel statusLabel = new JLabel("Unknown");
    private final Julti julti;
    private MinecraftInstance instance;

    public SingleInstancePanel(Julti julti) {
        this.julti = julti;
        setBorder(new FlatBorder());
        setLayout(new FlowLayout());
        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(2, 1));

        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        panel.add(statusLabel);
        add(panel);
        addMouseListener(this);
    }

    public void setInfo(MinecraftInstance instance) {
        nameLabel.setText(instance.getName());
        statusLabel.setText(instance.hasWindow() ? "Open" : "Closed");
        this.instance = instance;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == 1) {
            Thread.currentThread().setName("julti-gui");
            instance.activate(julti.getInstanceManager().getInstances().indexOf(instance) + 1);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            doPop(e);
        }
    }

    private void doPop(MouseEvent e) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (instance.hasWindow()) {
            GUIUtil.addMenuItem(popupMenu, "Close", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    instance.closeWindow();
                }
            });
        } else {
            GUIUtil.addMenuItem(popupMenu, "Launch", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    SafeInstanceLauncher.launchInstance(instance, julti);
                }
            });
        }
        GUIUtil.addMenuItem(popupMenu, "Open Folder", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                instance.openFolder();
            }
        });
        GUIUtil.addMenuItem(popupMenu, "Remove", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread.currentThread().setName("julti-gui");
                julti.getInstanceManager().removeInstance(instance);
                julti.storeLastInstances();
            }
        });
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            doPop(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}