package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
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
    private MinecraftInstance instance;

    public SingleInstancePanel() {
        this.setBorder(new FlatBorder());
        this.setLayout(new FlowLayout());
        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(2, 1));

        this.nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(this.nameLabel);
        panel.add(this.statusLabel);
        this.add(panel);
        this.addMouseListener(this);
    }

    public void setInfo(MinecraftInstance instance) {
        this.nameLabel.setText(instance.getName());
        this.statusLabel.setText(instance.hasWindow() ? "Open" : "Closed");
        this.instance = instance;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == 1) {
            Thread.currentThread().setName("julti-gui");
            Julti.doLater(() -> Julti.getJulti().activateInstance(this.instance));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            this.doPop(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            this.doPop(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    private void doPop(MouseEvent e) {
        JPopupMenu popupMenu = new JPopupMenu();
        if (this.instance.hasWindow()) {
            GUIUtil.addMenuItem(popupMenu, "Close", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    SingleInstancePanel.this.instance.closeWindow();
                }
            });
        } else {
            GUIUtil.addMenuItem(popupMenu, "Launch", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread.currentThread().setName("julti-gui");
                    SafeInstanceLauncher.launchInstance(SingleInstancePanel.this.instance);
                }
            });
        }
        GUIUtil.addMenuItem(popupMenu, "Open Folder", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SingleInstancePanel.this.instance.openFolder();
            }
        });
        GUIUtil.addMenuItem(popupMenu, "Remove", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread.currentThread().setName("julti-gui");
                Julti.waitForExecute(() -> InstanceManager.getInstanceManager().removeInstance(SingleInstancePanel.this.instance));
            }
        });
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }
}